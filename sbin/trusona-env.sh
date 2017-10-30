#!/bin/bash

mkdir -p ~/.gradle

echo "org.gradle.jvmargs=-Xmx2048M" >> ~/.gradle/gradle.properties

echo "trusonaUsername=${ARTIFACTORY_USERNAME}" >> ~/.gradle/gradle.properties
echo "trusonaPassword=${ARTIFACTORY_PASSWORD}" >> ~/.gradle/gradle.properties

echo "RELEASES_REPO=${RELEASES_REPO}" >> ~/.gradle/gradle.properties
echo "SNAPSHOTS_REPO=${SNAPSHOTS_REPO}" >> ~/.gradle/gradle.properties
echo "CONTEXT_REPO_ROOT=${CONTEXT_REPO_ROOT}" >> ~/.gradle/gradle.properties
