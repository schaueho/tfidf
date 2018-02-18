Complexity of the transducer version
====================================

tf-from-docs-xf worst case input
--------------------------------
For all documents |docs| with |td_i| being the number of terms for document `input`, we iterate over |tdi| at least once. We then also iterate twice over the number of all terms seen so far (`newtdcount`), once to determine the new termcount and once to build up the `termzeromap`. This then needs to be sorted again, which is at worst again an operation on all terms so far. We then merge all `tfs` vectors we have so far with this map, which again is an operation on all seen terms so far. In the worst case (last document), we have |newtdcount|=|terms| and |tfs|=|docs|, so we end up with:

<math>td-last * Tupdate + terms * Tcount + terms * Tzipmap + terms * Tsortop + docs * terms * Tmergeop</math>

As <math>td_last < terms</math>, the first three parts amount to <math>O(terms)</math>. As <math>3 << docs</math> for all practical purposes, this means the fourth part dominates `tf-from-docs-xf`. The interesting question is whether |docs| or |terms| is the more significant number. In a real-world usage, the number of terms is going to stabilize (flatten) with a linear increase of documents. Either way, we can assume <math>O(docs*terms)</math> as an upper bound. 

idf-xf worst case input
-----------------------
For each input, we're counting the documents (|docs|) and iterate over |terms| to compute the new idf vvalue. Finally, we're sorting the augmented term elements, which is dominated by |terms| again. So, for the last input, we end up with 

<math>docs * Tcount + terms * Tidfop + terms * Tsortop</math>

So <math>O(docs+terms)</math> is the upper bound here.

tfidf-xf worst case input
-------------------------
We iterate over all documents in the input (|docs|) and in an inner loop over all terms (|terms|). This is <math>O(docs²)</math> in the worst case.

Transducing over all documents
------------------------------
So far, we've only looked at the potentially last execution step of each `-xf` routine. But of course, to get there we will have called each of those for each document. The number of documents is the intereesting variable here, so we can treat the number of terms as constant and end up with:

<math>O(docs³*terms) + O(docs²+terms) + O(docs³) = O(docs³)</math>
