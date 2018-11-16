package org.wso2.carbon.apimgt.dbsync.dto;

import java.sql.Timestamp;

public class AuthorizationCodeDto {
    private int Id;
    private String codeId;
    private String authorizationCode;
    private int consumerKeyId;
    private String callbackUrl;
    private String scope;
    private String authzUser;
    private int tenantId;
    private String userDomain;
    private Timestamp timeCreated;
    private long validityPeriod;
    private String state;
    private String tokenId;
    private String subjectIdentifier;
    private String pkceCodeChallenge;
    private String pkceCodeChallengeMethod;
    private String authorizationCodeHash;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getCodeId() {
        return codeId;
    }

    public void setCodeId(String codeId) {
        this.codeId = codeId;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public int getConsumerKeyId() {
        return consumerKeyId;
    }

    public void setConsumerKeyId(int consumerKeyId) {
        this.consumerKeyId = consumerKeyId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
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

    public Timestamp getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Timestamp timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getSubjectIdentifier() {
        return subjectIdentifier;
    }

    public void setSubjectIdentifier(String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }

    public String getPkceCodeChallenge() {
        return pkceCodeChallenge;
    }

    public void setPkceCodeChallenge(String pkceCodeChallenge) {
        this.pkceCodeChallenge = pkceCodeChallenge;
    }

    public String getPkceCodeChallengeMethod() {
        return pkceCodeChallengeMethod;
    }

    public void setPkceCodeChallengeMethod(String pkceCodeChallengeMethod) {
        this.pkceCodeChallengeMethod = pkceCodeChallengeMethod;
    }

    public String getAuthorizationCodeHash() {
        return authorizationCodeHash;
    }

    public void setAuthorizationCodeHash(String authorizationCodeHash) {
        this.authorizationCodeHash = authorizationCodeHash;
    }

    @Override
    public String toString() {
        return Id + ":" + codeId + ":" + authorizationCode + ":" + consumerKeyId + ":" + callbackUrl + ":" + scope + ":"
                + authzUser + ":" + tenantId + ":" + userDomain + ":" + timeCreated + ":" + validityPeriod + ":" + state
                + ":" + tokenId + ":" + subjectIdentifier + ":" + pkceCodeChallenge + ":" + pkceCodeChallengeMethod
                + ":" + authorizationCodeHash;
    }
}
