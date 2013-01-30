package org.aksw.sparqlify.core.test;

import java.util.HashSet;
import java.util.Set;

import org.openjena.atlas.lib.Sink;

import com.hp.hpl.jena.sparql.core.Quad;

class SinkQuadsToSet
	implements Sink<Quad>
{
	private Set<Quad> quads = new HashSet<Quad>();
	
	@Override
	public void close() {
	}

	@Override
	public void send(Quad item) {
		quads.add(item);
	}

	@Override
	public void flush() {
	}

	public Set<Quad> getQuads() {
		return quads;
	}
}