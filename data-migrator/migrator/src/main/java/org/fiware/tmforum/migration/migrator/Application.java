package org.fiware.tmforum.migration.migrator;

import java.net.MalformedURLException;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "data-migrator", description = "Migration tool for transfering data between TMForum 0.32.x and 1.3.x", mixinStandardHelpOptions = true)
public class Application implements Runnable {

	@CommandLine.Option(names = {"-rB", "--readBroker"}, description = "Address of the broker to read from.", defaultValue = "http://localhost:8080")
	String readBroker;

	@CommandLine.Option(names = {"-wB", "--writeBroker"}, description = "Address of the broker to write from.", defaultValue = "http://localhost:1026")
	String writeBroker;

	@CommandLine.Option(names = {"-ll", "--legacyLoader"}, description = "Path to the jar-file of the legacy loader.", defaultValue = "data-migrator/legacy-loader/target/legacy-loader-0.1.jar")
	String legacyLoaderJar;

	@CommandLine.Option(names = {"-uw", "--updateWriter"}, description = "Path to the jar-file of the update writer.", defaultValue = "data-migrator/update-writer/target/update-writer-0.1.jar")
	String updateWriterJar;

	public static void main(String[] args) {
		CommandLine.run(new Application(), args);

	}

	@Override
	public void run() {
		Migrator migrator = new Migrator(readBroker, writeBroker, legacyLoaderJar, updateWriterJar);
		try {
			migrator.runUpdate();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		System.exit(0);
	}
}
