REM create a private key
openssl genrsa -out ca.key 2048
openssl rsa -in ca.key -out key.pem -outform PEM

REM create a SSL certificate
REM extensions required by apple https://support.apple.com/en-us/HT210176
openssl req -new -x509 -sha256 -days 365 -key ca.key -out server-ca.crt ^
	-subj "/C=US/ST=VA/L=Vienna/O=Nitm Org/OU=Nitm Proxy/CN=Nitmproxy CA Root" ^
    -addext "extendedKeyUsage = serverAuth"

REM export to PEM format
copy server-ca.crt server.pem
