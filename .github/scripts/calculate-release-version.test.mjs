import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

const script = join(
  process.cwd(),
  ".github/scripts/calculate-release-version.mjs",
);

const git = (directory, ...args) =>
  execFileSync("git", args, {
    cwd: directory,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  }).trim();

const commit = (directory, content) => {
  writeFileSync(join(directory, "content.txt"), content);
  git(directory, "add", "content.txt");
  git(directory, "commit", "-m", content);
  return git(directory, "rev-parse", "HEAD");
};

const repository = () => {
  const directory = mkdtempSync(join(tmpdir(), "release-version-"));
  git(directory, "init", "-b", "main");
  git(directory, "config", "user.name", "Release test");
  git(directory, "config", "user.email", "release-test@example.com");
  git(directory, "config", "commit.gpgsign", "false");
  git(directory, "config", "tag.gpgsign", "false");
  commit(directory, "first");
  return directory;
};

const calculate = (directory, increment = "patch") =>
  Object.fromEntries(
    execFileSync("node", [script, increment, "HEAD", "main"], {
      cwd: directory,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    })
      .trim()
      .split("\n")
      .map((line) => line.split("=")),
  );

test("the first release is v1.0.0", () => {
  const directory = repository();
  assert.equal(calculate(directory).tag, "v1.0.0");
});

test("increments stable SemVer tags explicitly", () => {
  const increments = [
    ["major", "v2.0.0"],
    ["minor", "v1.3.0"],
    ["patch", "v1.2.4"],
  ];

  for (const [increment, expected] of increments) {
    const directory = repository();
    git(directory, "tag", "v1.2.3");
    commit(directory, increment);
    assert.equal(calculate(directory, increment).tag, expected);
  }
});

test("reuses a SemVer tag already pointing at the release commit", () => {
  const directory = repository();
  git(directory, "tag", "--annotate", "v1.0.0", "--message", "Release v1.0.0");
  const result = calculate(directory);
  assert.equal(result.tag, "v1.0.0");
  assert.equal(result.already_tagged, "true");
});

test("rejects a release with no changes", () => {
  const directory = repository();
  git(directory, "tag", "v1.0.0");
  git(directory, "commit", "--allow-empty", "-m", "empty");

  assert.throws(
    () => calculate(directory),
    (error) => error.stderr.includes("There are no changes since v1.0.0"),
  );
});

test("rejects a commit outside main", () => {
  const directory = repository();
  git(directory, "checkout", "-b", "not-main");
  commit(directory, "branch change");

  assert.throws(
    () => calculate(directory),
    (error) => error.stderr.includes("is not on main"),
  );
});
