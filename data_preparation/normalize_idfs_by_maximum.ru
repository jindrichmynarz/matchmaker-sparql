PREFIX ex:  <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

WITH <http://linked.opendata.cz/resource/concept-scheme/cpv-2008/idf>
DELETE {
  ?cpv ex:idf ?_idf .
}
INSERT {
  ?cpv ex:idf ?idf .
}
WHERE {
  {
    SELECT (xsd:decimal(MAX(?idf)) AS ?maxIdf)
    WHERE {
      [] ex:idf ?idf .
    }
  }
  ?cpv ex:idf ?_idf .
  BIND (?_idf/?maxIdf AS ?idf)
}
