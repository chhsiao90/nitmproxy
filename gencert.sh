# create a private key
openssl genrsa -out ca.key 2048
openssl rsa -in ca.key -out key.pem -outform PEM

# create a SSL certificate
# extensions required by apple https://support.apple.com/en-us/HT210176
openssl req -new -x509 -sha256 -days 365 -key ca.key -out server-ca.cert \
	-subj "/C=US/ST=VA/L=Vienna/O=Nitm Org/OU=Nitm Proxy/CN=Nitmproxy CA Root" \
    -addext "extendedKeyUsage = serverAuth"

# save another copy with .pem extension
cp server-ca.cert server.pem
