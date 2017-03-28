# Virtuoso-specific SPARQL Update operation to compute IDFs of CPV concepts.
# It works only in Virtuoso, since it uses its built-in function `bif:log10`
# to compute logarithm. It expects CPV to be in the named graph
# <http://linked.opendata.cz/resource/concept-scheme/cpv-2008> and public
# contracts to be in <http://linked.opendata.cz/resource/dataset/isvz.cz>.
# The computed IDFs are stored in the named graph
# <http://linked.opendata.cz/resource/concept-scheme/cpv-2008/idf>.
# To normalize the IDFs into (0, 1> interval, use `normalize_idfs_by_maximum.ru`. 

PREFIX ex:   <http://example.com/>
PREFIX pc:   <http://purl.org/procurement/public-contracts#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>

INSERT {
  GRAPH <http://linked.opendata.cz/resource/concept-scheme/cpv-2008/idf> {
    ?cpv ex:idf ?idf .
  }
}
WHERE {
  {
    SELECT ?cpv (bif:log10(?n/(1.0 + COUNT(?contract))) AS ?idf)
    WHERE {
      {
        SELECT (COUNT(DISTINCT ?contract) AS ?n)
        WHERE {
          GRAPH <http://linked.opendata.cz/resource/dataset/isvz.cz> {
            ?contract (pc:mainObject|pc:additionalObject)/skos:closeMatch ?cpv .
          }
        }
      }
      {
        SELECT ?cpv
        WHERE {
          GRAPH <http://linked.opendata.cz/resource/concept-scheme/cpv-2008> {
            ?cpv skos:inScheme <http://linked.opendata.cz/resource/concept-scheme/cpv-2008> .
          }
        }
      }
      OPTIONAL {
        GRAPH <http://linked.opendata.cz/resource/dataset/isvz.cz> {
          ?contract (pc:mainObject|pc:additionalObject)/skos:closeMatch ?cpv .
        }
      }
    }
    GROUP BY ?cpv ?n
  }
}
