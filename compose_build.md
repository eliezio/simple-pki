### Docker Compose Build
```
docker-compose build --progress=plain
docker-compose up -d
docker-compose ps
docker-compose logs -f
```
### Generate CSR
```
openssl req -new -nodes -keyout /tmp/test.key -out /tmp/test.csr \
  -subj '/O=test/CN=test.com' \
  -addext 'basicConstraints=CA:false' \
  -addext 'keyUsage=digitalSignature,keyEncipherment' \
  -addext 'extendedKeyUsage=clientAuth,serverAuth' \
  -addext 'subjectAltName=IP:127.0.0.1,DNS:localhost'
```
```
openssl req -text -noout -in /tmp/test.csr
```
### Sign CSR
```
curl -sL "http://localhost:8080/pki/v1/certificates" \
  -H "Content-Type: application/x-pem-file" \
  -F "file=@/tmp/test.csr" \
  -o /tmp/test.crt \
  -D /tmp/test.crt_headers.txt
```
### Veriry Certificate
```
openssl x509 -text -noout -in /tmp/test.crt
```
```
openssl verify -CAfile <(curl -sL "http://localhost:8080/pki/v1/cacert") /tmp/test.crt
```
```
openssl verify -crl_check \
  -CAfile <(curl -sL "http://localhost:8080/pki/v1/cacert") \
  -CRLfile <(curl -sL "http://localhost:8080/pki/v1/crl") /tmp/test.crt
```
### Revoke Certificate
```
SN=$(grep X-Cert-Serial-Number /tmp/test.crt_headers.txt | tr -d '\r' | awk '{print $NF}')

curl -sL -X DELETE "http://localhost:8080/pki/v1/certificates/$SN"
```
### Veriry CRL
```
openssl verify -crl_check \
  -CAfile <(curl -sL "http://localhost:8080/pki/v1/cacert") \
  -CRLfile <(curl -sL "http://localhost:8080/pki/v1/crl") /tmp/test.crt
```
