(ns matvaretabellen.export
  (:require [matvaretabellen.core :as matvaretabellen]
            [powerpack.export :as export]))

(defn ^:export export [& _args]
  (set! *print-namespace-maps* false)
  (export/export! (matvaretabellen/create-build-app)))
