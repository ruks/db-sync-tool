package org.wso2.carbon.apimgt.dbsync;

import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.dbsync.dto.AccessTokenDto;
import org.wso2.carbon.apimgt.dbsync.dto.AuthorizationCodeDto;
import org.wso2.carbon.apimgt.dbsync.dto.TokenScopeDto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.ArrayList;
import java.util.Locale;

public class DBSynchronise {
    private static Logger logger = LoggerFactory.getLogger(DBSynchronise.class);

    private static String sourceDBUrl;
    private static String sourceDBUser;
    private static String sourceDBPass;
    private static String sourceDBDriver;

    private static String destDBUrl;
    private static String destDBUser;
    private static String destDBPass;
    private static String destDBDriver;

    private static int lastIndexOfAccessToken = -1;
    private static int lastIndexOfAuthCode = -1;

    public static void main(String[] args) {
        // resolve to issue 'ORA-12705: Cannot access NLS data files or invalid environment specified'
        Locale.setDefault(Locale.ENGLISH);

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

        if (args.length > 10) {
            if (!StringUtils.isBlank(args[8])) {
                lastIndexOfAccessToken = Integer.parseInt(args[8]);
            }
            if (!StringUtils.isBlank(args[9])) {
                lastIndexOfAuthCode = Integer.parseInt(args[9]);
            }
        } else if (args.length < 10) {
            int[] values = readIndexes();
            if (values != null) {
                lastIndexOfAccessToken = values[0];
                lastIndexOfAuthCode = values[1];
            } else {
                logger.warn("Indexes values is null. Continue with default values(-1,-1)");
            }
        } else {
            logger.error(
                    "Required arguments not provided. ex: jdbc:oracle:thin:amdb_200@localhost:1521/xe amdb_200 amdb_200 "
                            + "oracle.jdbc.OracleDriver jdbc:oracle:thin:amdb_250@localhost:1521/xe amdb_250 amdb_250 oracle.jdbc.OracleDriver");
            throw new RuntimeException("Required arguments not provided");
        }

        logger.info("Last index value of IDN_OAUTH2_ACCESS_TOKEN_SYNC is: " + lastIndexOfAccessToken);
        logger.info("Last index value of IDN_OAUTH2_AUTH_CODE_SYNC is: " + lastIndexOfAuthCode);

        DBSynchronise dbSynchronise = new DBSynchronise();

        ArrayList<AccessTokenDto> accessTokenDtos = dbSynchronise.readTokenInfo();
        logger.info("No of fetch IDN_OAUTH2_ACCESS_TOKEN records to be merged: " + accessTokenDtos.size());
        dbSynchronise.writeTokenInfo(accessTokenDtos);

        ArrayList<AuthorizationCodeDto> authorizationCodeDtos = dbSynchronise.readAuthCodes();
        logger.info("No of fetch IDN_OAUTH2_AUTHORIZATION_CODE records to be merged: " + authorizationCodeDtos.size());
        dbSynchronise.writeAuthCodes(authorizationCodeDtos);

        if (accessTokenDtos.size() > 0) {
            lastIndexOfAccessToken = accessTokenDtos.get(accessTokenDtos.size() - 1).getId();
        }
        if (authorizationCodeDtos.size() > 0) {
            lastIndexOfAuthCode = authorizationCodeDtos.get(authorizationCodeDtos.size() - 1).getId();
        }

        logger.info("Writing last processed index value for IDN_OAUTH2_ACCESS_TOKEN_SYNC as " + lastIndexOfAccessToken);
        logger.info("Writing last processed index value for IDN_OAUTH2_AUTH_CODE_SYNC as " + lastIndexOfAuthCode);
        writeIndexes(lastIndexOfAccessToken, lastIndexOfAuthCode);
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
                String accessTokenHashedJson = toHashedInfo(DigestUtils.sha256Hex(rs.getString("ACCESS_TOKEN")));
                String refreshTokenHashedJson = toHashedInfo(DigestUtils.sha256Hex(rs.getString("REFRESH_TOKEN")));
                dto.setAccessTokenHash(accessTokenHashedJson);
                dto.setRefreshTokenHash(refreshTokenHashedJson);
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
        Connection destinationConnection = null;
        Connection sourceConnection = null;
        try {
            Class.forName(destDBDriver);
            destinationConnection = DriverManager.getConnection(destDBUrl, destDBUser, destDBPass);
            destinationConnection.setAutoCommit(false);

            sourceConnection = DriverManager.getConnection(sourceDBUrl, sourceDBUser, sourceDBPass);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error Occurred while getting DB connection", e);
            return;
        }

        for (AccessTokenDto dto : accessTokenDtos) {
            try {
                logger.debug("Adding access token : " + dto.getAccessToken());
                insertIndividualTokenInfo(destinationConnection, dto);
                logger.info("Successfully Added access token : " + dto.getAccessToken());

                logger.debug("Adding record to IDN_OAUTH2_ACCESS_TOKEN_SCOPE: " + dto.toString());
                updateTokenScopeData(sourceConnection, destinationConnection, dto);
                logger.info("Successfully Added Scope for access token : " + dto.getAccessToken());

                logger.debug("Try to commit migrated token info for access token : " + dto.getAccessToken());
                destinationConnection.commit();
                logger.info("Successfully migrated token info for access token : " + dto.getAccessToken());
            } catch (SQLException e) {
                logger.debug("Error occurred while adding record : " + dto.toString(), e);
                logger.error("Error occurred while adding data for token: " + dto.getAccessToken());
            } finally {
                try {
                    destinationConnection.rollback();
                } catch (SQLException e) {
                    logger.error("SQL Exception occurred when rollback", e);
                }
            }
        }

        try {
            if (sourceConnection != null) {
                sourceConnection.close();
            }
            if (destinationConnection != null) {
                destinationConnection.close();
            }
        } catch (SQLException e) {
            logger.error("Connection close error", e);
        }
    }

    private void insertIndividualTokenInfo(Connection conn, AccessTokenDto accessTokenDto) throws SQLException {
        PreparedStatement insertStatement = null;
        String insertQuery = "MERGE INTO IDN_OAUTH2_ACCESS_TOKEN USING dual ON ( TOKEN_ID = ? )"
                + " WHEN MATCHED THEN UPDATE SET ACCESS_TOKEN=?,REFRESH_TOKEN=?,CONSUMER_KEY_ID=?,AUTHZ_USER=?,TENANT_ID=?,USER_DOMAIN=?,USER_TYPE=?,GRANT_TYPE=?,TIME_CREATED=?,"
                + "    REFRESH_TOKEN_TIME_CREATED=?,VALIDITY_PERIOD=?,REFRESH_TOKEN_VALIDITY_PERIOD=?,TOKEN_SCOPE_HASH=?,TOKEN_STATE=?,TOKEN_STATE_ID=?,SUBJECT_IDENTIFIER=?,"
                + " ACCESS_TOKEN_HASH=?,REFRESH_TOKEN_HASH=?"
                + " WHEN NOT MATCHED THEN INSERT VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            insertStatement = conn.prepareStatement(insertQuery);

        accessTokenDto.setSubjectIdentifier("a");
        try {

            insertStatement.setString(1, accessTokenDto.getTokenId());
            insertStatement.setString(2, accessTokenDto.getAccessToken());
            insertStatement.setString(3, accessTokenDto.getRefreshToken());
            insertStatement.setInt(4, accessTokenDto.getConsumerKeyId());
            insertStatement.setString(5, accessTokenDto.getAuthzUser());
            insertStatement.setInt(6, accessTokenDto.getTenantId());
            insertStatement.setString(7, accessTokenDto.getUserDomain());
            insertStatement.setString(8, accessTokenDto.getUserType());
            insertStatement.setString(9, accessTokenDto.getGrantType());
            insertStatement.setTimestamp(10, accessTokenDto.getTimeCreated());
            insertStatement.setTimestamp(11, accessTokenDto.getRefreshTokenTimeCreated());
            insertStatement.setLong(12, accessTokenDto.getValidityPeriod());
            insertStatement.setLong(13, accessTokenDto.getRefreshTokenValidityPeriod());
            insertStatement.setString(14, accessTokenDto.getTokenScopeHash());
            insertStatement.setString(15, accessTokenDto.getTokenState());
            insertStatement.setString(16, accessTokenDto.getTokenStateId());
            insertStatement.setString(17, accessTokenDto.getSubjectIdentifier());
            insertStatement.setString(18, accessTokenDto.getAccessTokenHash());
            insertStatement.setString(19, accessTokenDto.getRefreshTokenHash());

            insertStatement.setString(20, accessTokenDto.getTokenId());
            insertStatement.setString(21, accessTokenDto.getAccessToken());
            insertStatement.setString(22, accessTokenDto.getRefreshToken());
            insertStatement.setInt(23, accessTokenDto.getConsumerKeyId());
            insertStatement.setString(24, accessTokenDto.getAuthzUser());
            insertStatement.setInt(25, accessTokenDto.getTenantId());
            insertStatement.setString(26, accessTokenDto.getUserDomain());
            insertStatement.setString(27, accessTokenDto.getUserType());
            insertStatement.setString(28, accessTokenDto.getGrantType());
            insertStatement.setTimestamp(29, accessTokenDto.getTimeCreated());
            insertStatement.setTimestamp(30, accessTokenDto.getRefreshTokenTimeCreated());
            insertStatement.setLong(31, accessTokenDto.getValidityPeriod());
            insertStatement.setLong(32, accessTokenDto.getRefreshTokenValidityPeriod());
            insertStatement.setString(33, accessTokenDto.getTokenScopeHash());
            insertStatement.setString(34, accessTokenDto.getTokenState());
            insertStatement.setString(35, accessTokenDto.getTokenStateId());
            insertStatement.setString(36, accessTokenDto.getSubjectIdentifier());
            insertStatement.setString(37, accessTokenDto.getAccessTokenHash());
            insertStatement.setString(38, accessTokenDto.getRefreshTokenHash());
            logger.debug("Adding record to IDN_OAUTH2_ACCESS_TOKEN: " + accessTokenDto.toString());
            insertStatement.execute();
        } finally {
            if (insertStatement != null) {
                insertStatement.close();
            }
        }
    }

    private void updateTokenScopeData(Connection sourceConnection, Connection destinationConnection, AccessTokenDto tokenDto) throws SQLException {
        String sql = "SELECT * FROM IDN_OAUTH2_TOKEN_SCOPE_SYNC where TOKEN_ID = ? ORDER BY ID ASC";
        PreparedStatement readStatement = null;
        ResultSet rs = null;
        try {
            readStatement = sourceConnection.prepareStatement(sql);
            readStatement.setString(1, tokenDto.getTokenId());
            rs = readStatement.executeQuery();

            while (rs.next()) {
                TokenScopeDto dto = new TokenScopeDto();
                dto.setId(rs.getInt("ID"));
                dto.setTokenId(rs.getString("TOKEN_ID"));
                dto.setTokenScope(rs.getString("TOKEN_SCOPE"));
                dto.setTenantId(rs.getInt("TENANT_ID"));
                updateIndividualTokenScope(destinationConnection, dto);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (readStatement != null) {
                readStatement.close();
            }
        }
    }

    private void updateIndividualTokenScope(Connection conn, TokenScopeDto tokenScopeDto) throws SQLException {
        String insertQuery = "MERGE INTO IDN_OAUTH2_ACCESS_TOKEN_SCOPE USING dual ON (TOKEN_ID = ? and TOKEN_SCOPE = ?)"
                + " WHEN MATCHED THEN UPDATE SET TENANT_ID = ? WHEN NOT MATCHED THEN INSERT VALUES (?,?,?)";

        PreparedStatement insertStatement = null;
        try {
            insertStatement = conn.prepareStatement(insertQuery);
            insertStatement.setString(1, tokenScopeDto.getTokenId());
            insertStatement.setString(2, tokenScopeDto.getTokenScope());
            insertStatement.setInt(3, tokenScopeDto.getTenantId());
            insertStatement.setString(4, tokenScopeDto.getTokenId());
            insertStatement.setString(5, tokenScopeDto.getTokenScope());
            insertStatement.setInt(6, tokenScopeDto.getTenantId());
            logger.debug("Adding record to IDN_OAUTH2_ACCESS_TOKEN_SCOPE: " + tokenScopeDto.toString());
            insertStatement.execute();
        } finally {
            if (insertStatement != null) {
                insertStatement.close();
            }
        }
    }

    private ArrayList<AuthorizationCodeDto> readAuthCodes() {
        ArrayList<AuthorizationCodeDto> authorizationCodeDtos = new ArrayList<AuthorizationCodeDto>();
        Connection conn = null;
        PreparedStatement readStatement = null;
        try {
            Class.forName(sourceDBDriver);
            conn = DriverManager.getConnection(sourceDBUrl, sourceDBUser, sourceDBPass);
            String sql;
            sql = "SELECT * FROM IDN_OAUTH2_AUTH_CODE_SYNC where ID > ? ORDER BY ID ASC";
            readStatement = conn.prepareStatement(sql);
            readStatement.setInt(1, lastIndexOfAuthCode);
            ResultSet rs = readStatement.executeQuery();

            while (rs.next()) {
                AuthorizationCodeDto dto = new AuthorizationCodeDto();
                dto.setId(rs.getInt("ID"));
                dto.setCodeId(rs.getString("CODE_ID"));
                dto.setAuthorizationCode(rs.getString("AUTHORIZATION_CODE"));
                dto.setConsumerKeyId(rs.getInt("CONSUMER_KEY_ID"));
                dto.setCallbackUrl(rs.getString("CALLBACK_URL"));
                dto.setScope(rs.getString("SCOPE"));
                dto.setAuthzUser(rs.getString("AUTHZ_USER"));
                dto.setTenantId(rs.getInt("TENANT_ID"));
                dto.setUserDomain(rs.getString("USER_DOMAIN"));
                dto.setTimeCreated(rs.getTimestamp("TIME_CREATED"));
                dto.setValidityPeriod(rs.getLong("VALIDITY_PERIOD"));
                dto.setState(rs.getString("STATE"));
                dto.setTokenId(rs.getString("TOKEN_ID"));
                dto.setSubjectIdentifier(rs.getString("SUBJECT_IDENTIFIER"));
                dto.setPkceCodeChallenge(rs.getString("PKCE_CODE_CHALLENGE"));
                dto.setPkceCodeChallengeMethod(rs.getString("PKCE_CODE_CHALLENGE_METHOD"));
                String hashedJson = toHashedInfo(DigestUtils.sha256Hex(dto.getAuthorizationCode()));
                dto.setAuthorizationCodeHash(hashedJson);
                authorizationCodeDtos.add(dto);
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
        return authorizationCodeDtos;
    }

    private void writeAuthCodes(ArrayList<AuthorizationCodeDto> authorizationCodeDtos) {
        PreparedStatement insertStatement = null;
        String insertQuery = "MERGE INTO IDN_OAUTH2_AUTHORIZATION_CODE USING dual ON (CODE_ID = ?) "
                + "WHEN MATCHED THEN UPDATE SET "
                + "AUTHORIZATION_CODE = ?, CONSUMER_KEY_ID = ?, CALLBACK_URL = ?, SCOPE = ?, AUTHZ_USER = ?, TENANT_ID = ?, "
                + "USER_DOMAIN = ?, TIME_CREATED = ?, VALIDITY_PERIOD = ?, "
                + "STATE = ?, TOKEN_ID = ?, SUBJECT_IDENTIFIER = ?, PKCE_CODE_CHALLENGE = ?, PKCE_CODE_CHALLENGE_METHOD = ?, AUTHORIZATION_CODE_HASH = ? "
                + "WHEN NOT MATCHED THEN INSERT VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        Connection conn = null;
        try {
            Class.forName(destDBDriver);
            conn = DriverManager.getConnection(destDBUrl, destDBUser, destDBPass);
            insertStatement = conn.prepareStatement(insertQuery);
            for (AuthorizationCodeDto dto : authorizationCodeDtos) {
                insertStatement.setString(1, dto.getCodeId());
                insertStatement.setString(2, dto.getAuthorizationCode());
                insertStatement.setInt(3, dto.getConsumerKeyId());
                insertStatement.setString(4, dto.getCallbackUrl());
                insertStatement.setString(5, dto.getScope());
                insertStatement.setString(6, dto.getAuthzUser());
                insertStatement.setInt(7, dto.getTenantId());
                insertStatement.setString(8, dto.getUserDomain());
                insertStatement.setTimestamp(9, dto.getTimeCreated());
                insertStatement.setLong(10, dto.getValidityPeriod());
                insertStatement.setString(11, dto.getState());
                insertStatement.setString(12, dto.getTokenId());
                insertStatement.setString(13, dto.getSubjectIdentifier());
                insertStatement.setString(14, dto.getPkceCodeChallenge());
                insertStatement.setString(15, dto.getPkceCodeChallengeMethod());
                insertStatement.setString(16, dto.getAuthorizationCodeHash());

                insertStatement.setString(17, dto.getCodeId());
                insertStatement.setString(18, dto.getAuthorizationCode());
                insertStatement.setInt(19, dto.getConsumerKeyId());
                insertStatement.setString(20, dto.getCallbackUrl());
                insertStatement.setString(21, dto.getScope());
                insertStatement.setString(22, dto.getAuthzUser());
                insertStatement.setInt(23, dto.getTenantId());
                insertStatement.setString(24, dto.getUserDomain());
                insertStatement.setTimestamp(25, dto.getTimeCreated());
                insertStatement.setLong(26, dto.getValidityPeriod());
                insertStatement.setString(27, dto.getState());
                insertStatement.setString(28, dto.getTokenId());
                insertStatement.setString(29, dto.getSubjectIdentifier());
                insertStatement.setString(30, dto.getPkceCodeChallenge());
                insertStatement.setString(31, dto.getPkceCodeChallengeMethod());
                insertStatement.setString(32, dto.getAuthorizationCodeHash());
                logger.debug("Adding record to IDN_OAUTH2_AUTHORIZATION_CODE: " + dto.toString());
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
        int[] intData = new int[3];
        intData[0] = Integer.parseInt(data[0]);
        intData[1] = Integer.parseInt(data[1]);
        return intData;
    }

    private static void writeIndexes(int token, int authCode) {
        String contents = token + "," + authCode;
        try {
            FileUtils.writeStringToFile(new File("indexdata"), contents, Charset.defaultCharset(), false);
        } catch (IOException e) {
            logger.error("Error occurred while opening file. ", e);
        }
    }

    private String toHashedInfo(String hash) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("hash", hash);
        jsonObject.addProperty("algorithm", "SHA-256");
        return jsonObject.toString();
    }
}
