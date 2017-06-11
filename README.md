# Distributed wiki search

Search engine for https://github.com/ipfs/distributed-wikipedia-mirror

## Overview

This is a simple non-fuzzy search engine working on article titles.
It uses pre-computed IPLD hashmap to discover words.

The word map is built by taking list of article titles, splitting
them into individual words and adding article link to a dictionary for
each word with weight.

Weight of a word is computed by taking its length and dividing it
by combined length of words in a phrase.

A word can refer to multiple articles. Link to an article is a
(article, weight) tuple, weight being derived from the word
in the article title, computed as described above. Weights for links
are then normalized, so that sum of weights of all links in word entry
is equal to 1.0.

Example articles:
```
(A1) some thin
(A2) some else
(A3) some
(A4) else
```

Pre-normalized dict:
```
some: [A1: 0.5, A2: 0.5, A3: 1.0]
thin: [A1: 0.5]
else: [A3: 0.5, A4: 1.0]
```

Normalized dict:
```
some: [A1: 0.25, A2: 0.25, A3: 0.5]
thin: [A1: 1.0]
else: [A3: 0.3333..., A4: 0.6666...]
```

When a search is performed, a similar action is performed. The query is
split into words and weights for each of them are calculated.
Then link arrays are queried and a candidate map (article, relevance)
is created. Each entry queried from dict is multiplied by weight of
word by which it was queried.

Search:
```
some: [some: 1.0]

A3: 0.5 * 1.0 = 0.5 = most relevant
A1: 0.25 * 1.0 = 0.25
A2: 0.25 * 1.0 = 0.25
----
some thin: [some: 0.5, thin: 0.5]

A1: 0.25(some) * 0.5 + 1.0(thin) * 0.5 = 0.625 = most relevant
A3: 0.5 * 0.5 = 0.25
A2: 0.25 * 0.5 = 0.125
```

## TL;DR

Index is computed using word lengths in a way that popular words have
less weight. Search does similar thing, relevance is calculated by
multiplying index weights with search weights.

## Notes
The way this works now is probably far from perfect. There are some
cases where typing exact article name won't yield it as first result.

## Building new search index

1. Put article list(articles.gz) in this directory
2. Run ./index.sh
3. Copy resulting CID at the end.

## Building client

1. Direct build of js client requires hacky setup
2. Get js bundle from https://ipfs.io/ipfs/QmSoAZzmbY9VHcDCphM2ebsCcBiT2Q4N6kx8YJMHkbN38J/
3. Adapt js bootstrap to your needs (the one in index.html)

## License
MIT
