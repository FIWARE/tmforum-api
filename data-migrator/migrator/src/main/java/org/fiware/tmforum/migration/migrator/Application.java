package org.fiware.tmforum.migration.migrator;

import java.net.MalformedURLException;

//@CommandLine.Command(name = "data-migrator", description = "Migration tool for transfering data between TMForum 0.32.x and 1.3.x", mixinStandardHelpOptions = true)
public class Application {

	//	@CommandLine.Option(names = {"-rB", "--readBroker"}, description = "Address of the broker to read from.")
//	String readBroker;
//
//	@CommandLine.Option(names = {"-wB", "--writeBroker"}, description = "Address of the broker to write from.")
//	String writeBroker;
//
//	@CommandLine.Option(names = {"-ll", "--legacyLoader"}, description = "Path to the jar-file of the legacy loader.")
	private static String legacyLoaderJar = "data-migrator/legacy-loader/target/legacy-loader-0.1.jar";
	//
//	@CommandLine.Option(names = {"-uw", "--updateWriter"}, description = "Path to the jar-file of the update writer.")
	private static String updateWriterJar = "data-migrator/update-writer/target/update-writer-0.1.jar";

	public static void main(String[] args) {

		Migrator migrator = new Migrator("http://localhost:8080", "http://localhost:1026", legacyLoaderJar, updateWriterJar);
		try {
			migrator.runUpdate();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		System.exit(0);

	}

}
