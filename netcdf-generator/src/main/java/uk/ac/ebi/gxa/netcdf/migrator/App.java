package uk.ac.ebi.gxa.netcdf.migrator;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.*;

import java.util.Properties;

public class App {
    public static void main(String[] args) {
        SLF4JBridgeHandler.install();
        Logger log = LoggerFactory.getLogger(App.class);

        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        Option url = new Option("d", "url", true, "Database URL");
        url.setArgName("url");
        url.setRequired(true);
        Option username = new Option("u", "user", true, "Database username");
        username.setArgName("username");                               
        username.setRequired(true);
        Option password = new Option("p", "password", true, "Database password");
        password.setArgName("password");
        password.setRequired(true);
        Option netcdf = new Option("n", "netcdf", true, "NetCDF directory path");
        netcdf.setArgName("path");
        netcdf.setRequired(true);
        Option missing = new Option("m", "missingOnly", false, "Do not build existing NetCDFs");
        missing.setRequired(false);
        Option accession = new Option("a", "accession", true, "Build specific experiment accession");
        accession.setArgName("accession");
        missing.setRequired(false);

        options.addOption(url).addOption(username).addOption(password).addOption(netcdf).addOption(missing).addOption(accession);

        // Parse the arguments
        try {
            CommandLine commandLine = parser.parse(options, args);

            Properties props = new Properties();
            props.setProperty("jdbc.url", commandLine.getOptionValue('d'));
            props.setProperty("jdbc.username", commandLine.getOptionValue('u'));
            props.setProperty("jdbc.password", commandLine.getOptionValue('p'));
            props.setProperty("netcdf.path", commandLine.getOptionValue('n'));

            log.info(props.toString());

            XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans.xml"));
            PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
            cfg.setProperties(props);
            cfg.postProcessBeanFactory(factory);

            AtlasNetCDFMigrator service = (AtlasNetCDFMigrator)factory.getBean("service");

            if(commandLine.hasOption('a'))
                service.generateNetCDFForExperiment(commandLine.getOptionValue('a'), commandLine.hasOption('m'));
            else
                service.generateNetCDFForAllExperiments(commandLine.hasOption('m'));
        } catch(ParseException e) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setWidth(1000);
            helpFormatter.printHelp("java -Xmx2048m -jar netcdf-generator-X.XX-jar-with-dependencies.jar", "", options, "\n" + e.getMessage(), true);
            System.exit(-1);
        } catch(Throwable e) {
            log.error("Error", e);
            System.exit(-1);
        }

        System.exit(0);
    }
}