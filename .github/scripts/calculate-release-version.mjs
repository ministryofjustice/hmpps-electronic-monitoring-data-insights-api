#!/usr/bin/env node

import { appendFileSync } from 'node:fs'
import { execFileSync, spawnSync } from 'node:child_process'

const SEMVER_TAG = /^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/

const git = (...args) => execFileSync('git', args, { encoding: 'utf8' }).trim()

const gitStatus = (...args) => spawnSync('git', args, { stdio: 'ignore' }).status

const parseTag = tag => {
  const match = tag.match(SEMVER_TAG)
  return match ? match.slice(1).map(Number) : null
}

const compareTags = (left, right) => {
  const leftVersion = parseTag(left)
  const rightVersion = parseTag(right)

  for (let index = 0; index < 3; index += 1) {
    if (leftVersion[index] !== rightVersion[index]) return leftVersion[index] - rightVersion[index]
  }

  return 0
}

const fail = message => {
  throw new Error(message)
}

const [increment, sourceRef = 'HEAD', mainRef = 'origin/main'] = process.argv.slice(2)

if (!['major', 'minor', 'patch'].includes(increment)) {
  fail('The release increment must be major, minor or patch')
}

const sourceSha = git('rev-parse', `${sourceRef}^{commit}`)

if (gitStatus('merge-base', '--is-ancestor', sourceSha, mainRef) !== 0) {
  fail(`The release commit ${sourceSha} is not on ${mainRef}`)
}

const tags = git('tag', '--list')
  .split('\n')
  .filter(tag => SEMVER_TAG.test(tag))
  .sort(compareTags)

const tagsAtSource = git('tag', '--points-at', sourceSha)
  .split('\n')
  .filter(tag => SEMVER_TAG.test(tag))
  .sort(compareTags)

if (tagsAtSource.length > 1) {
  fail(`The release commit already has multiple SemVer tags: ${tagsAtSource.join(', ')}`)
}

let tag
let previousTag = ''
let alreadyTagged = false

if (tagsAtSource.length === 1) {
  const [existingTag] = tagsAtSource
  tag = existingTag
  alreadyTagged = true
  const tagIndex = tags.indexOf(tag)
  previousTag = tagIndex > 0 ? tags[tagIndex - 1] : ''
} else if (tags.length === 0) {
  tag = 'v1.0.0'
} else {
  const latestTag = tags[tags.length - 1]

  if (gitStatus('merge-base', '--is-ancestor', latestTag, sourceSha) !== 0) {
    fail(`The latest release tag ${latestTag} is not an ancestor of ${sourceSha}`)
  }

  const diffStatus = gitStatus('diff', '--quiet', latestTag, sourceSha)
  if (diffStatus === 0) fail(`There are no changes since ${latestTag}`)
  if (diffStatus !== 1) fail(`Unable to compare ${latestTag} with ${sourceSha}`)

  const [major, minor, patch] = parseTag(latestTag)
  const nextVersion = {
    major: [major + 1, 0, 0],
    minor: [major, minor + 1, 0],
    patch: [major, minor, patch + 1],
  }[increment]

  tag = `v${nextVersion.join('.')}`
  previousTag = latestTag

  if (tags.includes(tag)) fail(`The calculated release tag ${tag} already exists on another commit`)
}

const outputs = {
  tag,
  version: tag.slice(1),
  previous_tag: previousTag,
  source_sha: sourceSha,
  already_tagged: String(alreadyTagged),
}

if (process.env.GITHUB_OUTPUT) {
  appendFileSync(
    process.env.GITHUB_OUTPUT,
    `${Object.entries(outputs)
      .map(([key, value]) => `${key}=${value}`)
      .join('\n')}\n`,
  )
}

for (const [key, value] of Object.entries(outputs)) process.stdout.write(`${key}=${value}\n`)
