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

		Executable executable = new Executable(result, equipmentOn);

		executable.update(equipmentOn, this);
	}

	private class Executable implements IObserver<SolvisData> {

		private final SolvisData result;
		private final SolvisData equipmentOn;
		private long lastStartTime = -1;

		private int equipmentCntSinceLastSync = 0;
		private int syncCnt = 0;
		private long lastSyncedRunTime = -1;
		private long runTimeAfterLastSyncAfterOff = 0;
		private long runTimeAfterLastSyncCurrent = 0;
		private final Correction correction;

		private Executable(SolvisData result, SolvisData equipmentOn) {
			this.result = result;
			this.equipmentOn = equipmentOn;
			this.equipmentOn.registerContinuousObserver(this);
			this.result.registerContinuousObserver(this);
			if (RunTime.this.isCorrection()) {
				this.correction = result.getSolvis().getAllSolvisData().getCorrection(this.result.getId());
			} else {
				this.correction = null;
			}
		}

		private void updateSyncValues(long runTime) {
			this.lastSyncedRunTime = runTime;
			this.runTimeAfterLastSyncAfterOff = 0;
			this.runTimeAfterLastSyncCurrent = 0;
			this.equipmentCntSinceLastSync = 0;
		}

		private void correctionAdjust(SolvisData data, Object source) throws TypeException {

			if (source instanceof SystemBackup) {
				this.updateSyncValues((long) data.getInt() * 1000L);
				return;
			}

			if (!(source instanceof UpdateType)) {
				return;
			}

			if (!((UpdateType) source).isSyncType() && this.syncCnt == 0) {
				this.updateSyncValues((long) data.getInt() * 1000L);
				return;
			}

			if (!RunTime.this.isCorrection()) {
				return;
			}
			long syncedRunTime = (long) data.getInt() * 1000L;
			if (++this.syncCnt > 1) {
				int delta = (int) (syncedRunTime - this.lastSyncedRunTime - this.runTimeAfterLastSyncCurrent);
				this.correction.modify(delta, this.equipmentCntSinceLastSync);
			}
			this.updateSyncValues(syncedRunTime);
		}

		@Override
		public void update(SolvisData data, Object source) {

			try {

				Boolean equipmentOn = null;

				if (data.getDescription() == this.result.getDescription()) {
					if (source == this) {
						return;
					} else {
						equipmentOn = this.equipmentOn.getBool();
						this.correctionAdjust(data, source);
					}
				}

				if (equipmentOn == null) {
					equipmentOn = data.getBool();
				}

				if (equipmentOn && this.lastStartTime < 0) {
					++this.equipmentCntSinceLastSync;
				}

				if (equipmentOn || this.lastStartTime >= 0) {

					long time = data.getTimeStamp();

					int former = this.result.getInt();

					if (equipmentOn) {
						if (this.lastStartTime < 0) {
							this.lastStartTime = time;
						}
					}

					long runTime = time - this.lastStartTime;

					this.runTimeAfterLastSyncCurrent = runTime + this.runTimeAfterLastSyncAfterOff;
					int result;

					result = (int) ((this.lastSyncedRunTime + this.runTimeAfterLastSyncCurrent
							+ this.getCorrection(this.equipmentCntSinceLastSync) + 500L) / 1000L);

					if (result - former > 60 || !equipmentOn) {
						this.result.setInteger(result, data.getTimeStamp(), this);
					}

					if (!equipmentOn) {
						this.lastStartTime = -1;
						this.runTimeAfterLastSyncAfterOff = this.runTimeAfterLastSyncCurrent;
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
