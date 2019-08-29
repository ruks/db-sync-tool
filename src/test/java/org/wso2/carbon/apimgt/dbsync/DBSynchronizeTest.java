package org.wso2.carbon.apimgt.dbsync;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import java.util.Locale;

public class DBSynchronizeTest {

//    @Test
    public void testMainLocal() {
        PropertyConfigurator.configure(getClass().getClassLoader().getResourceAsStream("log4j.properties"));

        Locale.setDefault(Locale.ENGLISH);
        DBSynchronise dbSynchronise = new DBSynchronise();
        String[] pars = { "jdbc:oracle:thin:amdb_200@10.10.10.2:1521/xe", "amdb_200", "amdb_200",
                "oracle.jdbc.OracleDriver",

                "jdbc:oracle:thin:amdb_260@10.10.10.2:1521/xe", "amdb_260", "amdb_260", "oracle.jdbc.OracleDriver", };
        dbSynchronise.main(pars);
    }

//    @Test
    public void testMainRemote() {
        PropertyConfigurator.configure(getClass().getClassLoader().getResourceAsStream("log4j.properties"));

        Locale.setDefault(Locale.ENGLISH);
        DBSynchronise dbSynchronise = new DBSynchronise();
        String[] pars = { "jdbc:oracle:thin:amdb_200@192.168.104.32:1521/orcl", "amdb_200", "amdb_200",
                "oracle.jdbc.OracleDriver",

                "jdbc:oracle:thin:amdb_260@192.168.104.32:1521/orcl", "amdb_260", "amdb_260", "oracle.jdbc.OracleDriver", };
        dbSynchronise.main(pars);
    }
}
