package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil;

import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 * Date: 28/01/2013
 * Time: 17:31
 */
public class MatchSequence implements Matcher<StructuredStatement> {

    private final Matcher<StructuredStatement>[] inner;
    private final String name;  // useful for breakpointing.

    public MatchSequence(Matcher<StructuredStatement>... inner) {
        this.inner = inner;
        this.name = "";
    }

    public MatchSequence(String name, Matcher<StructuredStatement>... inner) {
        this.inner = inner;
        this.name = name;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        MatchIterator<StructuredStatement> mi = matchIterator.copy();

        for (Matcher<StructuredStatement> matcher : inner) {
            if (!matcher.match(mi, matchResultCollector)) return false;
        }

        matchIterator.advanceTo(mi);
        return true;
    }
}
