# Sparqlify Docker for the Sparqlify Web Admin


## Build

    docker build -t sparqlify .


## Run
 * Default Docker IP is 172.17.42.1 ; check with ip addr | grep docker

 * run one instance, open http://<docker ip>:8080/sparqlify in your browser:

   docker run -d -p 8060:8080 -p 8061:80 --name sparqlify sparqlify

 * Web interface should be available under [http://localhost:8061/sparqlify](http://localhost:8061/sparqlify)

 * run many times, open http://<docker ip>:<container port>/sparqlify in your browser:


    docker run --rm -P sparqlify
    docker ps


## ToDos

 * current config is a bad hack, even it works though

