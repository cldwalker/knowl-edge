(ns knowl.test.edge.base
  (:use [knowl.edge.base] :reload)
  (:use [knowl.edge.store.sesame] :reload)
  (:use midje.sweet))

(facts "An RDF Literal can be created."
      (l "lorem ipsum") => (knowl.edge.base.Literal. "lorem ipsum" nil nil)
      (l "lorem ipsum" "en") => (knowl.edge.base.Literal. "lorem ipsum" "en" nil)
      (l "lorem ipsum" :en) => (knowl.edge.base.Literal. "lorem ipsum" "en" nil)
      (l "lorem ipsum" 'en) => (knowl.edge.base.Literal. "lorem ipsum" "en" nil)
      (l "lorem ipsum" "http://example.com/datatype") => (knowl.edge.base.Literal. "lorem ipsum" nil (knowl.edge.base.URI. "http://example.com/datatype")))

(facts "An RDF BlankNode can be created."
      (b "loremipsumdolor") => (knowl.edge.base.BlankNode. "loremipsumdolor")
      (b "abc123 cde456") => (throws AssertionError)
      (b "") => (throws AssertionError)
      (b "loremipsumdolorsitametcunscurloremipsumdolorsitametcunscur") => (throws AssertionError)
      (:value (b)) => (just #"^[a-zA-Z0-9]{1,}$"))

(facts "An RDF URI can be created."
      (u "http://example.com/foo") => (knowl.edge.base.URI. "http://example.com/foo")
      (u "http://example.com/foo%20bar%20baz") => (knowl.edge.base.URI. "http://example.com/foo%20bar%20baz")
      (u :prefix1 "foo") => (knowl.edge.base.URI. "http://example1.com/foo")	
      (provided
        (resolve-prefix :prefix1) => "http://example1.com/")
      (u "prefix2" :foo) => (knowl.edge.base.URI. "http://example2.com/foo")
      (provided
        (resolve-prefix "prefix2") => "http://example2.com/")
      (u 'prefix3 'foo) => (knowl.edge.base.URI. "http://example3.com/foo")
      (provided
        (resolve-prefix 'prefix3) => "http://example3.com/"))

(facts "Applying str to a URI, BlankNode, or literal returns a plain string value."
      (:value (u "http://example.com/foo")) => "http://example.com/foo"
      (:value (b "loremipsumdolor")) => "loremipsumdolor"
      (:value (l "Lorem Ipsum" :en)) => "Lorem Ipsum")