BACKEND_DIR=${PWD}
cd ${BACKEND_DIR}

sudo systemctl stop backend.service


rm -f target/universal/backend-1.0.zip
rm -f -R target/universal/backend-1.0
sbt dist
unzip target/universal/backend-1.0.zip -d target/universal

cp -r ${BACKEND_DIR}/sh /home/local/backend/

sudo systemctl start backend.service
