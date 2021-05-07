#sudo openssl req -nodes -x509 -newkey rsa:4096 -keyout key.pem -out server.pem -days 180 
rm *.pem *.crt *.key
openssl genrsa -out ca.key 2048
openssl rsa -in ca.key -out key.pem -outform PEM
openssl req -sha256 -new -x509 -days 365 -key ca.key -out server.pem \
	-subj "/C=CN/ST=GD/L=SZ/O=lee/OU=study/CN=SafeKids"
#openssl x509 -inform DER -outform PEM -in server.crt -out server.pem
