import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Apr 26, 2010
 * Time: 1:12:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasInstaller {
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JdbcTemplate getJdbcTemplate() {
        if((null==jdbcTemplate)&&(null!=dataSource)){
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private DataSource dataSource = null;
    private JdbcTemplate jdbcTemplate = null;

    private void runProcess(String workingFolder, String ... args) throws Exception{

        ProcessBuilder pb = new ProcessBuilder(args); //,pathToInstall,connectionString);

        if(null!=workingFolder){
            pb.directory(new File(workingFolder));
        }

        Process child = pb.start();

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(child.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(child.getInputStream()));

        String s;
        while ((s = stdInput.readLine()) != null) {
                 System.out.println(s);
        }

        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        int retCode = child.waitFor();

        if(0!=retCode)
            throw new Exception(String.format("process executed returned %1$s",retCode));
    }

    public void install() throws Exception{
        Runtime rtime = Runtime.getRuntime();

        ClassPathResource r = new ClassPathResource("install.sh");

        //(new ClassPathResource("install.sh"))?
        String pathToInstall = getClass().getResource("install.sh").getPath();
        String workingFolder = getClass().getResource("").getPath();
        String connectionUrl = ((BasicDataSource) dataSource).getUrl();
        String connectionUsername =  ((BasicDataSource) dataSource).getUsername();
        String connectionPassword =  ((BasicDataSource) dataSource).getPassword();

        String connectionString =  String.format("%1$s/%2$s@%3$s"
                                                  ,connectionUsername
                                                  ,connectionPassword
                                                  ,connectionUrl);

        Pattern pattern = Pattern.compile("^jdbc:oracle:thin:@([\\w|.]+):(\\d+):(\\w+)$");
        Matcher matcher = pattern.matcher(connectionUrl);
        if(matcher.find()){
            String serverUrl = matcher.group(1);
            String serverPort = matcher.group(2);
            String serverServiceName = matcher.group(3);

            connectionString =  String.format("%1$s/%2$s@%3$s:%4$s/%5$s"
                                              ,connectionUsername
                                              ,connectionPassword
                                              ,serverUrl
                                              ,serverPort
                                              ,serverServiceName);
        }

        /*
        String command = String.format("sh %1$s %2$s"
                                       , pathToInstall
                                       , connectionString);
        */

        System.out.println(pathToInstall);

        String command = String.format("sh %1$s %2$s\n",pathToInstall,connectionString);
        command = String.format("chmod +x %1$s",pathToInstall);

        int retCode = 0;

        runProcess(null,"chmod","+x",pathToInstall);

        runProcess(workingFolder,pathToInstall,connectionString);
    }
}
