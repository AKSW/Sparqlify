package org.aksw.sparqlify.restriction.experiment;

public interface RestrictionOps {
	Restriction and(Restriction a, Restriction b);
	Restriction or(Restriction a, Restriction b);
	Restriction not(Restriction a, Restriction b);
}
