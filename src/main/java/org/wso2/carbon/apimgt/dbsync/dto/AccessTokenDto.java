package org.wso2.carbon.apimgt.dbsync.dto;

import java.sql.Timestamp;

public class AccessTokenDto {
    private int Id;
    private String tokenId;
    private String accessToken;
    private String refreshToken;
    private int consumerKeyId;
    private String authzUser;
    private int tenantId;
    private String userDomain;
    private String userType;
    private String grantType;
    private Timestamp timeCreated;
    private Timestamp refreshTokenTimeCreated;
    private long validityPeriod;
    private long refreshTokenValidityPeriod;
    private String tokenScopeHash;
    private String tokenState;
    private String tokenStateId;
    private String subjectIdentifier;
    private String accessTokenHash;
    private String refreshTokenHash;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getConsumerKeyId() {
        return consumerKeyId;
    }

    public void setConsumerKeyId(int consumerKeyId) {
        this.consumerKeyId = consumerKeyId;
    }

    public String getAuthzUser() {
        return authzUser;
    }

    public void setAuthzUser(String authzUser) {
        this.authzUser = authzUser;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public Timestamp getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Timestamp timeCreated) {
        this.timeCreated = timeCreated;
    }

    public Timestamp getRefreshTokenTimeCreated() {
        return refreshTokenTimeCreated;
    }

    public void setRefreshTokenTimeCreated(Timestamp refreshTokenTimeCreated) {
        this.refreshTokenTimeCreated = refreshTokenTimeCreated;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public long getRefreshTokenValidityPeriod() {
        return refreshTokenValidityPeriod;
    }

    public void setRefreshTokenValidityPeriod(long refreshTokenValidityPeriod) {
        this.refreshTokenValidityPeriod = refreshTokenValidityPeriod;
    }

    public String getTokenScopeHash() {
        return tokenScopeHash;
    }

    public void setTokenScopeHash(String tokenScopeHash) {
        this.tokenScopeHash = tokenScopeHash;
    }

    public String getTokenState() {
        return tokenState;
    }

    public void setTokenState(String tokenState) {
        this.tokenState = tokenState;
    }

    public String getTokenStateId() {
        return tokenStateId;
    }

    public void setTokenStateId(String tokenStateId) {
        this.tokenStateId = tokenStateId;
    }

    public String getSubjectIdentifier() {
        return subjectIdentifier;
    }

    public void setSubjectIdentifier(String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }

    public String getAccessTokenHash() {
        return accessTokenHash;
    }

    public void setAccessTokenHash(String accessTokenHash) {
        this.accessTokenHash = accessTokenHash;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    @Override
    public String toString() {
        return Id + ":" + tokenId + ":" + accessToken + ":" + refreshToken + ":" + consumerKeyId + ":" + authzUser + ":"
                + tenantId + ":" + userDomain + ":" + userType + ":" + grantType + ":" + timeCreated + ":"
                + refreshTokenTimeCreated + ":" + validityPeriod + ":" + refreshTokenValidityPeriod + ":"
                + tokenScopeHash + ":" + tokenState + ":" + tokenStateId + ":" + subjectIdentifier + ":"
                + accessTokenHash + ":" + refreshTokenHash;
    }
}

