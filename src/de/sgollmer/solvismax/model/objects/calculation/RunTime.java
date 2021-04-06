/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.backup.SystemBackup;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.update.Correction;
import de.sgollmer.solvismax.model.update.EquipmentOnOff.UpdateType;

public class RunTime extends Strategy<RunTime> {

	private static final ILogger logger = LogManager.getInstance().getLogger(RunTime.class);

	private RunTime(Calculation calculation) {
		super(calculation);
	}

	RunTime() {
		super(null);
	}

	@Override
	public RunTime create(Calculation calculation) {
		return new RunTime(calculation);
	}

	@Override
	boolean isWriteable() {
		return false;
	}

	@Override
	void instantiate(Solvis solvis) throws AssignmentException, AliasException {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getDescription().getId());

		AliasGroup aliasGroup = this.calculation.getAliasGroup();

		SolvisData equipmentOn = aliasGroup.get(allData, "equipmentOn");

		if (result == null || equipmentOn == null) {
			throw new AssignmentException("Assignment error: AliasGroup not assigned");
		}

		if (result.getSingleData() == null) {
			result.setInteger(0, -1);
		}

		Executable executable = new Executable(solvis, result, equipmentOn);

		try {
			executable.update(equipmentOn.getBool(), -1L);
		} catch (TypeException e) {
		}
	}

	private class Executable {

		private final SolvisData result;
		private final SolvisData equipmentOn;
		private final Solvis solvis;
		private long lastStartTime = -1;

		private int equipmentCntSinceLastSync = 0;
		private int syncCnt = -1;
		private long lastVerifiedRunTime = 0;
		private long runTimeAfterLastVerificationAfterOff = 0;
		private long runTimeAfterLastVerificationCurrent = 0;
		private final Correction correction;

		private Executable(Solvis solvis, SolvisData result, SolvisData equipmentOn) {
			this.solvis = solvis;
			this.result = result;
			this.equipmentOn = equipmentOn;
			this.equipmentOn.registerContinuousObserver(new IObserver<SolvisData>() {

				@Override
				public void update(SolvisData data, Object source) {
					Executable.this.updateByOnOff(data, source);

				}
			});
			this.result.registerContinuousObserver(new IObserver<SolvisData>() {

				@Override
				public void update(SolvisData data, Object source) {
					Executable.this.updateByValue(data, source);

				}
			});
			if (RunTime.this.isCorrection()) {
				this.correction = result.getSolvis().getAllSolvisData().getCorrection(this.result.getId());
			} else {
				this.correction = null;
			}
		}

		private void updateVerifiedValues(long runTime) {
			this.lastVerifiedRunTime = runTime;
			this.runTimeAfterLastVerificationAfterOff = 0;
			this.runTimeAfterLastVerificationCurrent = 0;
			this.equipmentCntSinceLastSync = 0;
		}

		private void correctionAdjust(SolvisData data, Object source) throws TypeException {

			long verifiedRunTime = (long) data.getInt() * 1000L;

			if (source instanceof SystemBackup) {
				this.updateVerifiedValues(verifiedRunTime);
				return;
			}
			if (!(source instanceof UpdateType)) {
				return;
			}
			
			UpdateType updateType = (UpdateType) source;

			if (this.syncCnt < 0) {
				this.syncCnt = 0;

				if (!updateType.isSyncType() || !RunTime.this.isCorrection()) {
					this.updateVerifiedValues(verifiedRunTime);
					return;
				}
			} else if (!RunTime.this.isCorrection() || !updateType.isSyncType()) {
				return;
			}

			++this.syncCnt;

			if (this.syncCnt > 1)

			{
				int delta = (int) (verifiedRunTime - this.lastVerifiedRunTime
						- this.runTimeAfterLastVerificationCurrent);
				this.correction.modify(delta, this.equipmentCntSinceLastSync);
			}
			this.updateVerifiedValues(verifiedRunTime);
		}

		private void updateByValue(SolvisData data, Object source) {

			if (source != this) {
				try {
					this.correctionAdjust(data, source);
					this.update(this.equipmentOn.getBool(), data.getTimeStamp());
				} catch (TypeException e) {
					logger.error("Type error, update ignored", e);
					return;
				}
			}
		}

		private void updateByOnOff(SolvisData data, Object source) {
			try {
				this.update(data.getBool(), data.getTimeStamp());
			} catch (TypeException e) {
				logger.error("Type error, update ignored", e);
				return;
			}
		}

		public void update(Boolean equipmentOn, long timeStamp) {

			try {

				if (equipmentOn && this.lastStartTime < 0) {
					++this.equipmentCntSinceLastSync;
				}

				if (equipmentOn || this.lastStartTime >= 0) {

					int former = this.result.getInt();

					if (equipmentOn) {
						if (this.lastStartTime < 0) {
							this.lastStartTime = timeStamp;
						}
					}

					long runTime = timeStamp - this.lastStartTime;

					this.runTimeAfterLastVerificationCurrent = runTime + this.runTimeAfterLastVerificationAfterOff;
					long result;

					result = this.lastVerifiedRunTime + this.runTimeAfterLastVerificationCurrent
							+ this.getCorrection(this.equipmentCntSinceLastSync);

					long interval = this.solvis.getDefaultReadMeasurementsInterval_ms();

					int toSet = (int) (result / interval * interval / 1000L); // abrunden

					if (Math.abs(toSet - former) > 60 || !equipmentOn) {
						this.result.setInteger(toSet, timeStamp, this);
					}

					if (!equipmentOn) {
						this.lastStartTime = -1;
						this.runTimeAfterLastVerificationAfterOff = this.runTimeAfterLastVerificationCurrent;
					}
				}
			} catch (

			TypeException e) {
				logger.error("Type error, update ignored", e);
				return;
			}

		}

		private long getCorrection(int cnt) {
			if (this.correction == null) {
				return 0L;
			} else {
				return this.correction.get(cnt);
			}
		}

	}

	@Override
	public void assign(SolvisDescription description) {
	}

	@Override
	Double getAccuracy() {
		return (double) 1;
	}

	@Override
	boolean isBoolean() {
		return false;
	}

}
