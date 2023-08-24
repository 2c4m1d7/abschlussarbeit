BACKEND_DIR=${PWD}
cd ${BACKEND_DIR}

sudo systemctl stop backend


rm -f target/universal/backend-1.0.zip
rm -f -R target/universal/backend-1.0
sbt dist
unzip target/universal/backend-1.0.zip -d target/universal

cp ${BACKEND_DIR}/sh target/universal/backend-1.0/

sudo systemctl start backend
