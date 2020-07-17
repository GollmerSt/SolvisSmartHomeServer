/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.windows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class Task {

	private static String ENCODING = "UTF-16LE";
	private static String VERSION = "1.0";
	private static String NAMESPACE = "http://schemas.microsoft.com/windows/2004/02/mit/task";
	private static String JAR = "SolvisSmartHomeServer.jar";

//	<?xml version="1.0" encoding="UTF-16"?>
//	<Task version="1.2" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task">
//		<RegistrationInfo>
//			<Author>Stefan Gollmer</Author>
//			<Description>Starts the SolvisSmartHomeServer</Description>
//			<URI>\SolvisSmartHomeServerTask</URI>
//		</RegistrationInfo>
//		<Triggers>
//			<BootTrigger>
//				<Enabled>true</Enabled>
//			</BootTrigger>
//		</Triggers>
//		<Principals>
//			<Principal id="Author">
//				<UserId>S-1-5-18</UserId>
//				<LogonType>Password</LogonType>
//				<RunLevel>LeastPrivilege</RunLevel>
//			</Principal>
//		</Principals>
//		<Settings>
//			<MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>
//			<DisallowStartIfOnBatteries>true</DisallowStartIfOnBatteries>
//			<StopIfGoingOnBatteries>true</StopIfGoingOnBatteries>
//			<AllowHardTerminate>true</AllowHardTerminate>
//			<StartWhenAvailable>false</StartWhenAvailable>
//			<RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>
//			<IdleSettings>
//				<StopOnIdleEnd>true</StopOnIdleEnd>
//				<RestartOnIdle>false</RestartOnIdle>
//			</IdleSettings>
//			<AllowStartOnDemand>true</AllowStartOnDemand>
//			<Enabled>true</Enabled>
//			<Hidden>false</Hidden>
//			<RunOnlyIfIdle>false</RunOnlyIfIdle>
//			<WakeToRun>false</WakeToRun>
//			<ExecutionTimeLimit>PT72H</ExecutionTimeLimit>
//			<Priority>7</Priority>
//		</Settings>
//		<Actions Context="Author">
//			<Exec>
//				<Command>javaw</Command>
//				<Arguments>-jar "C:\JavaPgms\SolvisSmartHomeServer\SolvisSmartHomeServer.jar"</Arguments>
//			</Exec>
//		</Actions>
//	</Task>

	public static void createTask(String taskFileName, boolean onBoot) throws XMLStreamException, IOException {

		File taskFile = new File(taskFileName);
		String path = System.getProperty("user.dir");
		String name = System.getProperty("user.name");

		String trigger;

		if (onBoot) {
			trigger = "BootTrigger";
		} else {
			trigger = "LogonTrigger";
		}

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		OutputStream outputStream = new FileOutputStream(taskFile);
		outputStream.write(0xff);
		outputStream.write(0xfe);
		XMLStreamWriter writer = factory.createXMLStreamWriter(outputStream, ENCODING);
		writer.setDefaultNamespace(NAMESPACE);
		writer.writeStartDocument(ENCODING, VERSION);
		writer.writeStartElement(NAMESPACE, "Task");
		writer.writeAttribute("version", "1.2");
		writer.writeNamespace("", NAMESPACE);
		{
			writer.writeStartElement("RegistrationInfo");
			{
				writer.writeStartElement("Author");
				{
					writer.writeCharacters(name);
				}
				writer.writeEndElement();
				writer.writeStartElement("Description");
				{
					writer.writeCharacters("Starts the SolvisSmartHomeServer");
				}
				writer.writeEndElement();
				writer.writeStartElement("URI");
				{
					writer.writeCharacters("\\SolvisSmartHomeServerTask");
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();

			writer.writeStartElement("Triggers");
			{
				writer.writeStartElement(trigger);
				{
					writer.writeStartElement("Enabled");
					{
						writer.writeCharacters("true");
					}
					writer.writeEndElement();
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();

			writer.writeStartElement("Principals");
			{
				writer.writeStartElement("Principal");
				writer.writeAttribute("id", "Author");
				{
					writer.writeStartElement("UserId");
					{
						writer.writeCharacters("S-1-5-18");
					}
					writer.writeEndElement();
					writer.writeStartElement("LogonType");
					{
						writer.writeCharacters("Password");
					}
					writer.writeEndElement();
					writer.writeStartElement("RunLevel");
					{
						writer.writeCharacters("LeastPrivilege");
					}
					writer.writeEndElement();
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();
			writer.writeStartElement("Settings");
			{
				writer.writeStartElement("MultipleInstancesPolicy");
				{
					writer.writeCharacters("IgnoreNew");
				}
				writer.writeEndElement();
				writer.writeStartElement("DisallowStartIfOnBatteries");
				{
					writer.writeCharacters("true");
				}
				writer.writeEndElement();
				writer.writeStartElement("StopIfGoingOnBatteries");
				{
					writer.writeCharacters("true");
				}
				writer.writeEndElement();
				writer.writeStartElement("AllowHardTerminate");
				{
					writer.writeCharacters("true");
				}
				writer.writeEndElement();
				writer.writeStartElement("StartWhenAvailable");
				{
					writer.writeCharacters("false");
				}
				writer.writeEndElement();
				writer.writeStartElement("RunOnlyIfNetworkAvailable");
				{
					writer.writeCharacters("false");
				}
				writer.writeEndElement();
				writer.writeStartElement("IdleSettings");
				{
					writer.writeStartElement("StopOnIdleEnd");
					{
						writer.writeCharacters("true");
					}
					writer.writeEndElement();
					writer.writeStartElement("RestartOnIdle");
					{
						writer.writeCharacters("false");
					}
					writer.writeEndElement();
				}
				writer.writeEndElement();
				writer.writeStartElement("AllowStartOnDemand");
				{
					writer.writeCharacters("true");
				}
				writer.writeEndElement();
				writer.writeStartElement("Enabled");
				{
					writer.writeCharacters("true");
				}
				writer.writeEndElement();
				writer.writeStartElement("Hidden");
				{
					writer.writeCharacters("false");
				}
				writer.writeEndElement();
				writer.writeStartElement("RunOnlyIfIdle");
				{
					writer.writeCharacters("false");
				}
				writer.writeEndElement();
				writer.writeStartElement("WakeToRun");
				{
					writer.writeCharacters("false");
				}
				writer.writeEndElement();
				writer.writeStartElement("ExecutionTimeLimit");
				{
					writer.writeCharacters("PT72H");
				}
				writer.writeEndElement();
				writer.writeStartElement("Priority");
				{
					writer.writeCharacters("7");
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();
			writer.writeStartElement("Actions");
			writer.writeAttribute("Context", "Author");
			{
				writer.writeStartElement("Exec");
				{
					writer.writeStartElement("Command");
					{
						writer.writeCharacters("javaw");
					}
					writer.writeEndElement();
					writer.writeStartElement("Arguments");
					{
						writer.writeCharacters("-jar \"" + path + File.separator + JAR + "\"");
					}
					writer.writeEndElement();
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();

		}
		writer.writeEndElement();
		writer.writeEndDocument();
		writer.flush();
		writer.close();
		outputStream.close();
	}
}