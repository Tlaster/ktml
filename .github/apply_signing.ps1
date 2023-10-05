$bytes = [Convert]::FromBase64String($Env:SIGNING_KEY)
[IO.File]::WriteAllBytes("./key.gpg", $bytes)
echo "signing.keyId=${Env:SIGNING_KEY_ID}
signing.password=${Env:SIGNING_PASSWORD}
signing.secretKeyRingFile=./key.gpg
ossrhUsername=${Env:OSSRH_USERNAME}
ossrhPassword=${Env:OSSRH_PASSWORD}" >publish.properties