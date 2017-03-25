# matchmaker-sparql

A SPARQL-based matchmaker for matching public contracts to bidders. It expects data to be described by the [Public Contracts Ontology](http://purl.org/procurement/public-contracts#).

## Usage

You can invoke the tool via [Leiningen](https://leiningen.org) or compile it by running `lein uberjar` and then executing the resulting JAR-file by Java. The only option you need to provide is a path to the configuration file:

```sh
lein run - --config config.edn
```

The configuration must be serialized using [EDN](https://github.com/edn-format/edn).

## License

Copyright © 2017 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0. 
