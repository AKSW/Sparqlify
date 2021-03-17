# Sparqlify SPARQL->SQL rewriter
[![Build Status](http://ci.aksw.org/jenkins/job/Sparqlify/badge/icon)](http://ci.aksw.org/jenkins/job/Sparqlify/)


## Introduction

Sparqlify is a scalable SPARQL-SQL rewriter whose development began in April 2011 in the course of the [LinkedGeoData](http://linkedgeodata.org) project.

This system's features/traits are:
* Support of the ['Sparqlification Mapping Language' (SML)](http://sparqlify.org/wiki/SML), an intuitive language for expressing RDB-RDF mappings with only very little syntactic noise.
* Scalability: Sparqlify does not evaluate expressions in memory. All SPARQL filters end up in the corresponding SQL statement, giving the underlying RDBMS has maximum control over query planning.
* A powerful rewriting engine that analyzes filter expressions in order to eleminate self joins and joins with unsatisfiable conditions.
* Initial support for spatial datatypes and predicates.
* A subset of the SPARQL 1.0 query language plus sub queries are supported.
* Tested with PostgreSQL/Postgis and H2. Support for further databases is planned.
* CSV support
* R2RML will be supported soon

## Supported SPARQL language features
* Join, LeftJoin (i.e. Optional), Union, Sub queries
* Filter predicates: comparison: (<=, <, =, >, >=) logical: (!, &&; ||) arithmetic: (+, -) spatial: st\_intersects, geomFromText; other: regex, lang, langMatches  
* Aggregate functions: Count(\*)
* Order By is pushed into the SQL


## Debian packages

Sparqlify Debian packages can be obtained by following means:
* Via the [Linked Data Stack](http://stack.linkeddata.org) (recommended)
* Download from the [Sparqlify website's download section](http://sparqlify.org/downloads/releases).
* Directly from source using maven (read down the README)

### Public repositories

After setting up any of the repositories below, you can install sparqlify with apt using

* apt: `sudo apt-get install sparqlify-cli`

#### Linked Data Stack (this is what you want)

Sparqlify is distributed at the [Linked Data Stack](http://stack.linkeddata.org), which offers many great tools done by various contributors of the Semantic Web community.

* The repository is available in the flavors `nightly`, `testing` and `stable` [here](http://stack.linkeddata.org/download/repo.php).

```bash
# !!! Replace stable with nightly or testing as needed !!!

# Download the repository package
wget http://stack.linkeddata.org/ldstable-repository.deb

# Install the repository package
sudo dpkg -i ldstable-repository.deb

# Update the repository database
sudo apt-get update
```


#### Bleeding Edge (Not recommended for production)
For the latest development version (built on every commit) perform the following steps

Import the public key with

    wget -qO - http://cstadler.aksw.org/repos/apt/conf/packages.precise.gpg.key  | sudo apt-key add -

Add the repository

    echo 'deb http://cstadler.aksw.org/repos/apt precise main contrib non-free' | sudo tee -a /etc/apt/sources.list.d/cstadler.aksw.org.list


Note that this also works with distros other than "precise" (ubuntu 12.04) such as ubuntu 14.04 or 16.04.



## Building
Building the repository creates the JAR files providing the `sparqlify-*` tool suite.


### Debian package
Building debian packages from this repo relies on the [Debian Maven Plugin](http://debian-maven.sourceforge.net]) plugin, which requires a debian-compatible environment.
If such an environment is present, the rest is simple:

    # Install all shell scripts necessary for creating deb packages
    sudo apt-get install devscripts

    # Execute the follwing from the `<repository-root>/sparqlify-core` folder:
    mvn clean install deb:package

    # Upon sucessful completion, the debian package is located under `<repository-root>/sparqlify-core/target`
    # Install using `dpkg`
    sudo dpkg -i sparqlify_<version>.deb

    # Uninstall using dpkg or apt:
    sudo dpkg -r sparqlify
    sudo apt-get remove sparqlify


### Assembly based
Another way to build the project is run the following commands at `<repository-root>`

    mvn clean install

    cd sparqlify-cli
    mvn assembly:assembly


This will generate a single stand-alone jar containing all necessary dependencies.
Afterwards, the shell scripts under `sparqlify-core/bin` should work.

## Tool suite

If Sparqlify was installed from the debian package, the following commands are available system-wide:

* `sparqlify`: This is the main executable for running individual SPARQL queries, creating dumps and starting a stand-alone server.
* `sparqlify-csv`: This tool can create RDF dumps from CSV file based on SML view definitions.
* `sparqlify-platform`: A stand-alone server component integrating additional projects.

These tools write their output (such as RDF data in the N-TRIPLES format) to STDOUT. Log output goes to STDERR.

### sparqlify
Usage: `sparqlify [options]`

Options are:

* Setup
  * -m   SML view definition file

* Database Connectivity Settings
  * -h   Hostname of the database (e.g. localhost or localhost:5432)
  * -d   Database name
  * -u   User name
  * -p   Password
  * -j   JDBC URI (mutually exclusive with both -h and -d)

* Quality of Service
  * -n   Maximum result set size
  * -t   Maximum query execution time in seconds (excluding rewriting time)

* Stand-alone Server Configuration
  * -P   Server port [default: 7531]

* Run-Once (these options prevent the server from being started and are mutually exclusive with the server configuration)
  * -D   Create an N-TRIPLES RDF dump on STDOUT 
  * -Q   [SPARQL query] Runs a SPARQL query against the configured database and view definitions

#### Example
The following command will start the Sparqlify HTTP server on the default port.

    sparqlify -h localhost -u postgres -p secret -d mydb -m mydb-mappings.sml -n 1000 -t 30

Agents can now access the SPARQL endpoint at `http://localhost:7531/sparql`

### sparqlify-csv
Usage: `sparqlify-csv [options]`

* Setup
  * -m   SML view definition file
  * -f   Input data file
  * -v   View name (can be omitted if the view definition file only contains a single view)

* CSV Parser Settings
  * -d   CSV field delimiter (default is '"')
  * -e   CSV field escape delimiter (escapes the field delimiter) (default is '\')
  * -s   CSV field separator (default is ',')
  * -h   Use first row as headers. This option allows one to reference columns by name additionally to its index.


### sparqlify-platform (Deprecated; about to be superseded by sparqlify-web-admin)
The Sparqlify Platform (under /sparqlify-platform) bundles Sparqlify with the Linked Data wrapper [Pubby](https://github.com/cygri/pubby) and the SPARQL Web interface [Snorql](https://github.com/kurtjx/SNORQL).

Usage: `sparqlify-platform config-dir [port]` 

* `config-dir` Path to the configuration directory, e.g. `<repository-root/sparqlify-platform/config/example>`
* `port` Port on which to run the platform, default 7531.


For building, at the root of the project (outside of the sparqlify-\* directories), run `mvn compile` to build all modules.
Afterwards, lauch the platform using:

    cd sparqlify-platform/bin
    ./sparqlify-platform <path-to-config> <port>


Assuming the platform runs under `http://localhost:7531`, you can access the following services relative to this base url:
* `/sparql` is Sparqlify's SPARQL endpoint
* `/snorql` shows the SNORQL web frontend
* `/pubby` is the entry point to the Linked Data interface


#### Configuration
The configDirectory argument is mandatory and must contain a *sub-directory* for the context-path (i.e. `sparqlify-platform`) in turn contains the files:
* `platform.properties` This file contains configuration parameters that can be adjusted, such as the database connection.
* `views.sparqlify` The set of Sparqlify view definition to use.

I recommend to first create a copy of the files in `/sparqlify-platform/config/example` under a different location, then adjust the parameters and finally launch the platform with `-DconfigDirectory=...` set appropriately.

The platform *applies autoconfiguration to Pubby and Snorql*:
* Snorql: Namespaces are those of the views.sparqlify file.
* Pubby: The host name of all resources generated in the Sparqlify views is replaced with the URL of the platform (currently still needs to be configured via `platform.properties`)

Additionally you probably want to make the URIs nice by e.g. configuring an apache reverse proxy:

Enable the apache `proxy_http` module:

	sudo a2enmod proxy_http

Then in your `/etc/apache2/sites-available/default` add lines such as

	ProxyRequest Off
	ProxyPass /resource http://localhost:7531/pubby/bizer/bsbm/v01/ retry=1
	ProxyPassReverse /resource http://localhost:7531/pubby/bizer/bsbm/v01/

These entries will enable requests to `http://localhost/resource/...` rather than `http//localhost:7531/pubby/bizer/bsbm/v01/`.

The `retry=1` means, that apache only waits 1 seconds before retrying again when it encounters an error (e.g. HTTP code 500) from the proxied resource.

*IMPORTANT: ProxyRequests are off by default; DO NOT ENABLE THEM UNLESS YOU KNOW WHAT YOU ARE DOING. Simply enabling them potentially allows anyone to use your computer as a proxy.*


## SML Mapping Syntax:
A Sparqlification Mapping Language (SML) configuration is essentially a set of CREATE VIEW statements, somewhat similar to the CREATE VIEW statement from SQL.
Probably the easiest way to learn to syntax is to look at the following resources:

* The [SML documentation](http://sparqlify.org/wiki/SML)
* The [SML test suite](https://github.com/AKSW/Sparqlify/tree/master/sparqlify-core/src/test/resources/org/aksw/sml/r2rml_tests) which is derived from the [R2RML test suite](https://github.com/AKSW/Sparqlify/tree/master/sparqlify-core/src/test/resources/org/w3c/r2rml_tests).

Two more examples are from

Additionally, for convenience, prefixes can be declared, which are valid throughout the config file.
As comments, you can use //, /\* \*/, and #. 

For a first impression, here is a quick example:
    
    /* This is a comment
     * /* You can even nest them! */
     */
    // Prefixes are valid throughout the file
    Prefix dbp:<http://dbpedia.org/ontology/>
    Prefix ex:<http://ex.org/>

    Create View myFirstView As
        Construct {
            ?s a dbp:Person .
            ?s ex:workPage ?w .
        }
    With
        ?s = uri('http://mydomain.org/person', ?id) // Define ?s to be an URI generated from the concatenation of a prefix with mytable's id-column.
        ?w = uri(?work_page) // ?w is assigned the URIs in the column 'work_page' of 'mytable'
    Constrain
        ?w prefix "http://my-organization.org/user/" // Constraints can be used for optimization, e.g. to prune unsatisfiable join conditions
    From
        mytable; // If you want to use an SQL query, the query (without trailing semicolon) must be enclosed in double square brackets: [[SELECT id, work_page FROM mytable]]


### Notes for sparqlify-csv
For `sparqlify-csv` view definition syntax is almost the same as above; the differences being:

* Instead of `Create View viewname As Construct` start your views with `CREATE VIEW TEMPLATE viewname As Construct`
* There is no FROM and CONSTRAINT clause

Colums can be referenced either by name (see the -h option) or by index (1-based).

#### Example

    // Assume a CSV file with the following columns (osm stands for OpenStreetMap)
    (city\_name, country\_name, osm\_entity\_type, osm\_id, longitude, latitude)

    Prefix fn:<http://aksw.org/sparqlify/> //Needed for urlEncode and urlDecode.
    Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
    Prefix owl:<http://www.w3.org/2002/07/owl#>
    Prefix xsd:<http://www.w3.org/2001/XMLSchema#>
    Prefix geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>

    Create View Template geocode As
      Construct {
        ?cityUri
          owl:sameAs ?lgdUri .

        ?lgdUri
          rdfs:label ?cityLabel ;
          geo:long ?long ;
          geo:lat ?lat .
      }
      With
        ?cityUri = uri(concat("http://fp7-pp.publicdata.eu/resource/city/", fn:urlEncode(?2), "-", fn:urlEncode(?1)))
        ?cityLabel = plainLiteral(?1)
        ?lgdUri = uri(concat("http://linkedgeodata.org/triplify/", ?4, ?5))
        ?long = typedLiteral(?6, xsd:float)
        ?lat = typedLiteral(?7, xsd:float)



