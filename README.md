# matchmaker-sparql

A SPARQL-based matchmaker for matching public contracts to bidders. It expects data to be described by the [Public Contracts Ontology](http://purl.org/procurement/public-contracts#). This matchmaker is a streamlined reimplementation of the previously developed [matchmaker](https://github.com/opendatacz/matchmaker).

## Usage

You can invoke the tool via [Leiningen](https://leiningen.org) or compile it by running `lein uberjar` and then executing the resulting JAR-file by Java. The only option you need to provide is a path to the configuration file:

```sh
lein run - --config config.edn
```

The configuration must be serialized using [EDN](https://github.com/edn-format/edn).
A sample configuration file is [here](resources/sample_config.edn).
Evaluation results produced by the tool are written to a file `resuls_{uuid}.edn` in the current directory.

## Data preparation

The matchmaker expects [Common Procurement Vocabulary](https://github.com/opendatacz/cpv2rdf) (CPV) to be in the named graph `<http://linked.opendata.cz/resource/concept-scheme/cpv-2008>`. Some matchmakers, such as those using query expansion, use inverse document frequencies (IDF) of CPV concepts. These IDFs can be produced by running SPARQL Update operations in `/resources/data_preparation`.

## License

Copyright © 2017 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0. 
