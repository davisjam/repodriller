package org.repodriller.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.repodriller.RepositoryMining;
import org.repodriller.filter.commit.OnlyInMainBranch;
import org.repodriller.filter.range.Commits;
import org.repodriller.scm.GitRepository;

import junit.framework.Assert;

public class ConcurrencyTest {

	private String path1;
	private static final int nCommitsVisited = 21;
	private static List<Map<String, Integer>> results;

	@BeforeClass
	public static void setUp() {
		System.out.println("SET UP");
		results = new ArrayList<Map<String, Integer>>();
	}

	@Before
	public void setPath() {
		path1 = this.getClass().getResource("/").getPath() + "../../test-repos/libuv";
	}

	@Test
	public void sequential() {
		TSTestVisitor visitor = new TSTestVisitor();

		new RepositoryMining()
		.in(GitRepository.singleProject(path1))
		.through(Commits.daily(5))
		.process(visitor)
		.filters(new OnlyInMainBranch())
		.visitorsAreThreadSafe(true)
		.visitorsChangeRepoState(false)
		.withThreads(1)
		.mine();

		addVisitorToResults(visitor);
	}

	@Test
	public void simpleConcurrent() {
		TSTestVisitor visitor = new TSTestVisitor();

		new RepositoryMining()
		.in(GitRepository.singleProject(path1))
		.through(Commits.daily(5))
		.process(visitor)
		.filters(new OnlyInMainBranch())
		.visitorsAreThreadSafe(true)
		.visitorsChangeRepoState(false)
		.withThreads(2) // >1 thread
		.mine();

		addVisitorToResults(visitor);
	}

	@Test
	public void heavyConcurrent() {
		TSTestVisitor visitor = new TSTestVisitor();

		new RepositoryMining()
		.in(GitRepository.singleProject(path1))
		.through(Commits.daily(5))
		.process(visitor)
		.filters(new OnlyInMainBranch())
		.visitorsAreThreadSafe(true)
		.visitorsChangeRepoState(false)
		.withThreads(100) // Many threads
		.mine();

		addVisitorToResults(visitor);
	}

	@Test
	public void changingRepoState_Sequential() {
		TSCheckoutTestVisitor visitor = new TSCheckoutTestVisitor();

		new RepositoryMining()
		.in(GitRepository.singleProject(path1))
		.through(Commits.daily(5))
		.process(visitor)
		.filters(new OnlyInMainBranch())
		.visitorsAreThreadSafe(true)
		.visitorsChangeRepoState(true) // Clone for each thread
		.withThreads(1) // One thread
		.mine();

		addVisitorToResults(visitor);
	}

	@Test
	public void hangingRepoState_Concurrent() {
		TSCheckoutTestVisitor visitor = new TSCheckoutTestVisitor();

		new RepositoryMining()
		.in(GitRepository.singleProject(path1))
		.through(Commits.daily(5))
		.process(visitor)
		.filters(new OnlyInMainBranch())
		.visitorsAreThreadSafe(true)
		.visitorsChangeRepoState(true) // Clone for each thread
		.withThreads(20) // Many threads
		.mine();

		addVisitorToResults(visitor);
	}

	@Test
	public void defaultThreads() {
		TSTestVisitor visitor = new TSTestVisitor();

		new RepositoryMining()
		.in(GitRepository.singleProject(path1))
		.through(Commits.daily(5))
		.process(visitor)
		.filters(new OnlyInMainBranch())
		.visitorsAreThreadSafe(true)
		.visitorsChangeRepoState(false)
		.withThreads(-1) // Default # threads
		.mine();

		addVisitorToResults(visitor);
	}

	@Test
	public void catchesInvalidConfig() {
		TSTestVisitor visitor = new TSTestVisitor();
		boolean threw = false;

		try {
			new RepositoryMining()
			.in(GitRepository.singleProject(path1))
			.through(Commits.daily(5))
			.process(visitor)
			.filters(new OnlyInMainBranch())
			.visitorsAreThreadSafe(false) // Non-thread safe
			.withThreads(2) // but 2 threads
			.mine();

			addVisitorToResults(visitor);
		} catch (Exception e) {
			threw = true;
		}

		Assert.assertTrue(threw);
	}


	@AfterClass
	public static void checkResults() {
		/* Ensure the first result is correct and the other results match it. */
		Map<String, Integer> firstResult = results.remove(0);
		System.out.println("firstResult: nHashesVisited " + firstResult.get("nHashesVisited"));
		Assert.assertTrue(firstResult.get("nHashesVisited") == nCommitsVisited);
		Assert.assertTrue(firstResult.get("nTimesVisited") == nCommitsVisited);
		Assert.assertTrue(firstResult.get("nCommitsVisited") == nCommitsVisited);

		results.forEach(result -> {
			System.out.println("nSubsequent result: HashesVisited " + firstResult.get("nHashesVisited"));
//			Assert.assertEquals(firstResult.get("nHashesVisited"), result.get("nHashesVisited"));
//			Assert.assertEquals(firstResult.get("nTimesVisited"), result.get("nTimesVisited"));
//			Assert.assertEquals(firstResult.get("nCommitsVisited"), result.get("nCommitsVisited"));
		});
	}

	private void addVisitorToResults(TSTestVisitor visitor) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		result.put("nHashesVisited", visitor.getVisitedHashes().size());
		result.put("nTimesVisited", visitor.getVisitedCommits().size());
		result.put("nCommitsVisited", visitor.getVisitedCommits().size());
		results.add(result);
	}

	private void addVisitorToResults(TSCheckoutTestVisitor visitor) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		result.put("nHashesVisited", visitor.getVisitedHashes().size());
		result.put("nTimesVisited", visitor.getVisitedCommits().size());
		result.put("nCommitsVisited", visitor.getVisitedCommits().size());
		results.add(result);
	}
}
