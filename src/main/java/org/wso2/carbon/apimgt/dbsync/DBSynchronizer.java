package org.wso2.carbon.apimgt.dbsync;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.dbsync.dto.AccessTokenDto;
import org.wso2.carbon.apimgt.dbsync.dto.TokenScopeDto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.ArrayList;

public class DBSynchronizer {
    private static Logger logger = LoggerFactory.getLogger(DBSynchronizer.class);

    private static String sourceDBUrl;
    private static String sourceDBUser;
    private static String sourceDBPass;
    private static String sourceDBDriver;

    private static String destDBUrl;
    private static String destDBUser;
    private static String destDBPass;
    private static String destDBDriver;

    private static int lastIndexOfAccessToken = -1;
    private static int lastIndexOfAccessTokenScope = -1;

    public static void main(String[] args) {
        PropertyConfigurator.configure("log4j.properties");
        logger.info("starting database sync operation");
        sourceDBUrl = args[0];
        sourceDBUser = args[1];
        sourceDBPass = args[2];
        sourceDBDriver = args[3];

        destDBUrl = args[4];
        destDBUser = args[5];
        destDBPass = args[6];
        destDBDriver = args[7];

        if (args.length > 9) {
            if (!StringUtils.isBlank(args[8])) {
                lastIndexOfAccessToken = Integer.parseInt(args[8]);
            }
            if (!StringUtils.isBlank(args[9])) {
                lastIndexOfAccessTokenScope = Integer.parseInt(args[9]);
            }
        } else if (args.length < 9) {
            int[] values = readIndexes();
            if (values != null) {
                lastIndexOfAccessToken = values[0];
                lastIndexOfAccessTokenScope = values[1];
            } else {
                logger.warn("Indexes values is null.Continue with default values(-1,-1)");
            }
        } else {
            logger.error(
                    "Required arguments not provided. ex: jdbc:oracle:thin:amdb_200@localhost:1521/xe amdb_200 amdb_200 oracle.jdbc.OracleDriver jdbc:oracle:thin:amdb_250@localhost:1521/xe amdb_250 amdb_250 oracle.jdbc.OracleDriver");
            throw new RuntimeException("Required arguments not provided");
        }

        logger.info("Last index value of IDN_OAUTH2_ACCESS_TOKEN is: " + lastIndexOfAccessToken);
        logger.info("Last index value of IDN_OAUTH2_ACCESS_TOKEN_SCOPE is: " + lastIndexOfAccessTokenScope);
        DBSynchronizer dbSynchronizer = new DBSynchronizer();
        ArrayList<AccessTokenDto> accessTokenDtos = dbSynchronizer.readTokenInfo();
        logger.info("No of fetch IDN_OAUTH2_ACCESS_TOKEN records to be merged: " + accessTokenDtos.size());
        dbSynchronizer.writeTokenInfo(accessTokenDtos);
        ArrayList<TokenScopeDto> tokenScopeDtos = dbSynchronizer.readTokenScope();
        logger.info("No of fetch IDN_OAUTH2_ACCESS_TOKEN_SCOPE records to be merged: " + tokenScopeDtos.size());
        dbSynchronizer.writeTokenScope(tokenScopeDtos);

        if (accessTokenDtos.size() > 0) {
            lastIndexOfAccessToken = accessTokenDtos.get(accessTokenDtos.size() - 1).getId();
        }
        if (tokenScopeDtos.size() > 0) {
            lastIndexOfAccessTokenScope = tokenScopeDtos.get(tokenScopeDtos.size() - 1).getId();
        }

        logger.info("Writing last processed index value for IDN_OAUTH2_ACCESS_TOKEN as " + lastIndexOfAccessToken);
        logger.info("Writing last processed index value for IDN_OAUTH2_ACCESS_TOKEN_SCOPE as "
                + lastIndexOfAccessTokenScope);
        writeIndexes(lastIndexOfAccessToken, lastIndexOfAccessTokenScope);
    }

    private ArrayList<AccessTokenDto> readTokenInfo() {
        ArrayList<AccessTokenDto> accessTokenDtos = new ArrayList<AccessTokenDto>();
        Connection conn = null;
        PreparedStatement readStatement = null;
        try {
            Class.forName(sourceDBDriver);
            conn = DriverManager.getConnection(sourceDBUrl, sourceDBUser, sourceDBPass);
            String sql = "SELECT * FROM IDN_OAUTH2_ACCESS_TOKEN_SYNC where ID > ? ORDER BY ID ASC";
            readStatement = conn.prepareStatement(sql);
            readStatement.setInt(1, lastIndexOfAccessToken);
            ResultSet rs = readStatement.executeQuery();
            while (rs.next()) {
                AccessTokenDto dto = new AccessTokenDto();
                dto.setId(rs.getInt("ID"));
                dto.setTokenId(rs.getString("TOKEN_ID"));
                dto.setAccessToken(rs.getString("ACCESS_TOKEN"));
                dto.setRefreshToken(rs.getString("REFRESH_TOKEN"));
                dto.setConsumerKeyId(rs.getInt("CONSUMER_KEY_ID"));
                dto.setAuthzUser(rs.getString("AUTHZ_USER"));
                dto.setTenantId(rs.getInt("TENANT_ID"));
                dto.setUserDomain(rs.getString("USER_DOMAIN"));
                dto.setUserType(rs.getString("USER_TYPE"));
                dto.setGrantType(rs.getString("GRANT_TYPE"));
                dto.setTimeCreated(rs.getTimestamp("TIME_CREATED"));
                dto.setRefreshTokenTimeCreated(rs.getTimestamp("REFRESH_TOKEN_TIME_CREATED"));
                dto.setValidityPeriod(rs.getLong("VALIDITY_PERIOD"));
                dto.setRefreshTokenValidityPeriod(rs.getLong("REFRESH_TOKEN_VALIDITY_PERIOD"));
                dto.setTokenScopeHash(rs.getString("TOKEN_SCOPE_HASH"));
                dto.setTokenState(rs.getString("TOKEN_STATE"));
                dto.setTokenStateId(rs.getString("TOKEN_STATE_ID"));
                dto.setSubjectIdentifier(rs.getString("SUBJECT_IDENTIFIER"));
                dto.setAccessTokenHash(DigestUtils.sha256Hex(rs.getString("ACCESS_TOKEN")));
                dto.setRefreshTokenHash(DigestUtils.sha256Hex(rs.getString("REFRESH_TOKEN")));
                accessTokenDtos.add(dto);
            }
            rs.close();
            readStatement.close();
            conn.close();
        } catch (SQLException e) {
            logger.error("SQL Exception occurred", e);
        } catch (ClassNotFoundException e) {
            logger.error("Database Driver not found", e);
        } finally {
            try {
                if (readStatement != null)
                    readStatement.close();
            } catch (SQLException e) {
                logger.error("SQL Exception occurred when closing statement", e);
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                logger.error("Connection close error", e);
            }
        }
        return accessTokenDtos;
    }

    private void writeTokenInfo(ArrayList<AccessTokenDto> accessTokenDtos) {
        PreparedStatement insertStatement = null;
        String insertQuery = "MERGE INTO IDN_OAUTH2_ACCESS_TOKEN USING dual ON ( TOKEN_ID = ? )"
                + " WHEN MATCHED THEN UPDATE SET ACCESS_TOKEN=?,REFRESH_TOKEN=?,CONSUMER_KEY_ID=?,AUTHZ_USER=?,TENANT_ID=?,USER_DOMAIN=?,USER_TYPE=?,GRANT_TYPE=?,TIME_CREATED=?,"
                + "    REFRESH_TOKEN_TIME_CREATED=?,VALIDITY_PERIOD=?,REFRESH_TOKEN_VALIDITY_PERIOD=?,TOKEN_SCOPE_HASH=?,TOKEN_STATE=?,TOKEN_STATE_ID=?,SUBJECT_IDENTIFIER=?,"
                + " ACCESS_TOKEN_HASH=?,REFRESH_TOKEN_HASH=?"
                + " WHEN NOT MATCHED THEN INSERT VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Connection conn = null;
        try {
            Class.forName(destDBDriver);
            conn = DriverManager.getConnection(destDBUrl, destDBUser, destDBPass);
            insertStatement = conn.prepareStatement(insertQuery);
            for (AccessTokenDto dto : accessTokenDtos) {
                dto.setSubjectIdentifier("a");
                insertStatement.setString(1, dto.getTokenId());
                insertStatement.setString(2, dto.getAccessToken());
                insertStatement.setString(3, dto.getRefreshToken());
                insertStatement.setInt(4, dto.getConsumerKeyId());
                insertStatement.setString(5, dto.getAuthzUser());
                insertStatement.setInt(6, dto.getTenantId());
                insertStatement.setString(7, dto.getUserDomain());
                insertStatement.setString(8, dto.getUserType());
                insertStatement.setString(9, dto.getGrantType());
                insertStatement.setTimestamp(10, dto.getTimeCreated());
                insertStatement.setTimestamp(11, dto.getRefreshTokenTimeCreated());
                insertStatement.setLong(12, dto.getValidityPeriod());
                insertStatement.setLong(13, dto.getRefreshTokenValidityPeriod());
                insertStatement.setString(14, dto.getTokenScopeHash());
                insertStatement.setString(15, dto.getTokenState());
                insertStatement.setString(16, dto.getTokenStateId());
                insertStatement.setString(17, dto.getSubjectIdentifier());
                insertStatement.setString(18, dto.getAccessTokenHash());
                insertStatement.setString(19, dto.getRefreshTokenHash());

                insertStatement.setString(20, dto.getTokenId());
                insertStatement.setString(21, dto.getAccessToken());
                insertStatement.setString(22, dto.getRefreshToken());
                insertStatement.setInt(23, dto.getConsumerKeyId());
                insertStatement.setString(24, dto.getAuthzUser());
                insertStatement.setInt(25, dto.getTenantId());
                insertStatement.setString(26, dto.getUserDomain());
                insertStatement.setString(27, dto.getUserType());
                insertStatement.setString(28, dto.getGrantType());
                insertStatement.setTimestamp(29, dto.getTimeCreated());
                insertStatement.setTimestamp(30, dto.getRefreshTokenTimeCreated());
                insertStatement.setLong(31, dto.getValidityPeriod());
                insertStatement.setLong(32, dto.getRefreshTokenValidityPeriod());
                insertStatement.setString(33, dto.getTokenScopeHash());
                insertStatement.setString(34, dto.getTokenState());
                insertStatement.setString(35, dto.getTokenStateId());
                insertStatement.setString(36, dto.getSubjectIdentifier());
                insertStatement.setString(37, dto.getAccessTokenHash());
                insertStatement.setString(38, dto.getRefreshTokenHash());
                logger.debug("Adding record to IDN_OAUTH2_ACCESS_TOKEN: " + dto.toString());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            logger.error("SQL Exception occurred", e);
        } catch (ClassNotFoundException e) {
            logger.error("Database Driver not found", e);
        } finally {
            try {
                if (insertStatement != null)
                    insertStatement.close();
            } catch (SQLException e) {
                logger.error("SQL Exception occurred when closing statement", e);
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                logger.error("Connection close error", e);
            }
        }
    }

    private ArrayList<TokenScopeDto> readTokenScope() {
        ArrayList<TokenScopeDto> tokenScopeDtos = new ArrayList<TokenScopeDto>();
        Connection conn = null;
        PreparedStatement readStatement = null;
        try {
            Class.forName(sourceDBDriver);
            conn = DriverManager.getConnection(sourceDBUrl, sourceDBUser, sourceDBPass);
            String sql;
            sql = "SELECT * FROM IDN_OAUTH2_TOKEN_SCOPE_SYNC where ID > ? ORDER BY ID ASC";
            readStatement = conn.prepareStatement(sql);
            readStatement.setInt(1, lastIndexOfAccessTokenScope);
            ResultSet rs = readStatement.executeQuery();

            while (rs.next()) {
                TokenScopeDto dto = new TokenScopeDto();
                dto.setId(rs.getInt("ID"));
                dto.setTokenId(rs.getString("TOKEN_ID"));
                dto.setTokenScope(rs.getString("TOKEN_SCOPE"));
                dto.setTenantId(rs.getInt("TENANT_ID"));
                tokenScopeDtos.add(dto);
            }
            rs.close();
            readStatement.close();
            conn.close();
        } catch (SQLException e) {
            logger.error("SQL Exception occurred", e);
        } catch (ClassNotFoundException e) {
            logger.error("Database Driver not found", e);
        } finally {
            try {
                if (readStatement != null)
                    readStatement.close();
            } catch (SQLException e) {
                logger.error("SQL Exception occurred when closing statement", e);
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                logger.error("Connection close error", e);
            }
        }
        return tokenScopeDtos;
    }

    private void writeTokenScope(ArrayList<TokenScopeDto> tokenScopeDtos) {
        PreparedStatement insertStatement = null;
        String insertQuery = "MERGE INTO IDN_OAUTH2_ACCESS_TOKEN_SCOPE USING dual ON (TOKEN_ID = ? and TOKEN_SCOPE = ?)"
                + " WHEN MATCHED THEN UPDATE SET TENANT_ID = ? WHEN NOT MATCHED THEN INSERT VALUES (?,?,?)";

        Connection conn = null;
        try {
            Class.forName(destDBDriver);
            conn = DriverManager.getConnection(destDBUrl, destDBUser, destDBPass);
            insertStatement = conn.prepareStatement(insertQuery);
            for (TokenScopeDto dto : tokenScopeDtos) {
                insertStatement.setString(1, dto.getTokenId());
                insertStatement.setString(2, dto.getTokenScope());
                insertStatement.setInt(3, dto.getTenantId());
                insertStatement.setString(4, dto.getTokenId());
                insertStatement.setString(5, dto.getTokenScope());
                insertStatement.setInt(6, dto.getTenantId());
                logger.debug("Adding record to IDN_OAUTH2_ACCESS_TOKEN_SCOPE: " + dto.toString());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            logger.error("SQL Exception occurred", e);
        } catch (ClassNotFoundException e) {
            logger.error("Database Driver not found", e);
        } finally {
            try {
                if (insertStatement != null) {
                    insertStatement.close();
                }
            } catch (SQLException e) {
                logger.error("SQL Exception occurred when closing statement", e);
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                logger.error("Connection close error", e);
            }
        }
    }

    private static int[] readIndexes() {
        String contents;
        try {
            contents = FileUtils.readFileToString(new File("indexdata"), "UTF-8");
        } catch (IOException e) {
            logger.error("Error occurred while opening file. ", e);
            return null;
        }
        if (StringUtils.isBlank(contents)) {
            return null;
        }
        String[] data = contents.trim().split(",");
        if (data.length < 2) {
            return null;
        }
        int[] intData = new int[2];
        intData[0] = Integer.parseInt(data[0]);
        intData[1] = Integer.parseInt(data[1]);
        return intData;
    }

    private static void writeIndexes(int token, int scope) {
        String contents = token + "," + scope;
        try {
            FileUtils.writeStringToFile(new File("indexdata"), contents, Charset.defaultCharset(), false);
        } catch (IOException e) {
            logger.error("Error occurred while opening file. ", e);
        }
    }
}
