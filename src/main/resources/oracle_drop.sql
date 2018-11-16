drop trigger table_token_insert;
drop trigger table_token_scope_insert;
drop trigger table_auth_code_insert;

drop trigger token_sync_trig;
drop trigger token_scope_sync_trig;
drop trigger auth_code_sync_trig;

drop SEQUENCE token_seq;
drop SEQUENCE token_scope_seq;
drop SEQUENCE auth_code_seq;

drop table IDN_OAUTH2_ACCESS_TOKEN_SYNC;
drop table IDN_OAUTH2_TOKEN_SCOPE_SYNC;
drop table IDN_OAUTH2_AUTH_CODE_SYNC;