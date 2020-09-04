import groovy.transform.Field
import groovy.util.Eval

@Field String[] topics = []
@Field Map topicsMap = [:]

pipeline {
	agent {
		label 'master'
	}
	options {
		buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '30'))
		timestamps()
		disableConcurrentBuilds()
	}
	parameters {
		string(name: 'GIT_USR', defaultValue: 'oc-ops-service-git', description: '')
		string(name: 'GIT_PSW', defaultValue: '', description: '')
		string(name: 'ORG_REPO_URL', defaultValue: 'https://api.github.com/orgs/oncorps/repos', description: '')
		string(name: 'BRANCH_NAME', defaultValue: 'master', description: '')
		string(name: 'FOLDER_PATH', defaultValue: '/var/lib/jenkins/oncorps_repositories/', description: '')
		string(name: 'DOCKER_BINARY_PATH', defaultValue: '/usr/bin/docker', description: '')
		string(name: 'SNYK_PSW', defaultValue: '', description: '')
		string(name: 'TOPICS', description: 'Tech Stack List', defaultValue: 'php,javascript,typescript')
		string(name: 'ORG_NAME', description: 'Organization Name', defaultValue: 'OnCorps')
	}
    environment {
        GIT = credentials('oc-ops-service-git')
        SNYK = credentials('snyk_laurent-douchy')
    }

    stages {
        stage('Store Repository Data') {
            steps {
                script {
                    topics = params.TOPICS.split(',')
                    for (int i = 0; i < topics.size(); i++) {
                        List<String> repos = []
                        def countResponse = shellCommandOutput("""
                            curl -u '${GIT_USR}:${GIT_PSW}' \
                            'https://api.github.com/orgs/oncorps'\
                            | jq  '.total_private_repos'
                            """
                                )
                        int total_private_repos = countResponse
                        int maxLimit = ((total_private_repos / 30) + 1)
                        int page = 1
                        while (page <= maxLimit) {
                            def response = shellCommandOutput("""
                            curl -u '${GIT_USR}:${GIT_PSW}' -H Accept:application/vnd.github.mercy-preview+json\
                            '${params.ORG_REPO_URL}?page=${page}'\
                            | jq -r '.[]|select(.topics | contains(["${topics[i]}"]))|.name'
                            """
                                    )
                            if (!(response.size() == 0)) {
                                repos.addAll(response.split('\n'))
                            }
                            page++
                        }
                        println 'Repository List for ${topics[i]} : ' + repos
                        topicsMap[topics[i]] = repos
                    }
                }
            }
        }
        stage('Initiate Scan') {
            steps {
                script {
                    for (int i = 0; i < topics.size(); i++) {
                        List<String> filteredRepos = topicsMap[topics[i]]
                        List<String> filteredByTags = []
                        List<String> restOfRepos = []
                        Map reposWithSemanticTags = [: ]
                        stage('Filter by Tag : ' + topics[i]) {
                            script {
                                filteredRepos.each {
                                    def REPO_NAME = it
                                    String TAGS_URL = "https://api.github.com/repos/${params.ORG_NAME}/${REPO_NAME}/tags"
                                    def response = shellCommandOutput("""
                                    curl -u '${GIT_USR}:${GIT_PSW}' \
                                    '${TAGS_URL}'\
                                    | jq -r '.[]|.name'
                                    """
                                            )
                                    if (!(response.size() == 0)) {
                                        List<String> REPO_TAGS = response.split('\n')
                                        String regex = "([0-9]+)\\.([0-9]+)\\.([0-9]+)"
                                        List<String> validTagList = extractSubListUsingRegex(regex, REPO_TAGS)
                                        int validTagCount = validTagList.size()
                                        if (validTagCount > 0) {
                                            validTagList = validTagList.sort()
                                            println REPO_NAME
                                            println 'Valid Tag List >>> ' + validTagList
                                            println 'Most recent version >>>>>' + validTagList.get((validTagCount - 1))
                                            filteredByTags.add(REPO_NAME)
                                            reposWithSemanticTags[REPO_NAME] = validTagList.get((validTagCount - 1))
                                        }
                                    }
                                }
                                println 'Repo Map containing Tag' + reposWithSemanticTags
                                println 'Repo List for topic' + filteredRepos
                                restOfRepos = filteredRepos - filteredByTags
                                println 'Rest of Repos' + restOfRepos
                            }
                        }

                        stage('Start Parallel Build for Topic : ' + topics[i]) {
                            script {
                                def branches = [: ]
                                // add entries for Scan with Tags
                                reposWithSemanticTags.each { repo, tag ->
                                    String REPO_NAME = repo
                                    String TAG = tag
                                    String CLONE_URL = "https://${GIT_USR}:${GIT_PSW}@github.com/${params.ORG_NAME}/${REPO_NAME}.git"
                                    branches['Scan - ' + REPO_NAME + ' - tag : ' + String.valueOf(TAG)] = {
                                        build(
                                                job: 'snyk_scanner',
                                                propagate: false,
                                                parameters : [
                                                    string(name: 'CLONE_URL', value: String.valueOf(CLONE_URL)),
                                                    string(name: 'PROJECT_NAME', value: String.valueOf(REPO_NAME)),
                                                    string(name: 'BRANCH_NAME', value: String.valueOf(BRANCH_NAME)),
                                                    string(name: 'FOLDER_PATH', value: String.valueOf(FOLDER_PATH)),
                                                    string(name: 'DOCKER_BINARY_PATH', value: String.valueOf(DOCKER_BINARY_PATH)),
                                                    string(name: 'SNYK_PSW', value: String.valueOf(SNYK_PSW)),
                                                    string(name: 'TOPIC', value: String.valueOf(topics[i])),
                                                    string(name: 'TAG', value: String.valueOf(TAG))
                                                ])
                                    }
                                }
                                // add entries for Scan without Tags
                                restOfRepos.each {
                                    String REPO_NAME = it
                                    String CLONE_URL = "https://${GIT_USR}:${GIT_PSW}@github.com/${params.ORG_NAME}/${REPO_NAME}.git"
                                    branches['Scan - ' + REPO_NAME] = {
                                        build(
                                                job: 'snyk_scanner',
                                                propagate: false,
                                                parameters : [
                                                    string(name: 'CLONE_URL', value: String.valueOf(CLONE_URL)),
                                                    string(name: 'PROJECT_NAME', value: String.valueOf(REPO_NAME)),
                                                    string(name: 'BRANCH_NAME', value: String.valueOf(BRANCH_NAME)),
                                                    string(name: 'FOLDER_PATH', value: String.valueOf(FOLDER_PATH)),
                                                    string(name: 'DOCKER_BINARY_PATH', value: String.valueOf(DOCKER_BINARY_PATH)),
                                                    string(name: 'SNYK_PSW', value: String.valueOf(SNYK_PSW)),
                                                    string(name: 'TOPIC', value: String.valueOf(topics[i]))
                                                ])
                                    }
                                }
                                parallel branches
                            }
                        }
                    }
                }
            }
        }
    }
}

String shellCommandOutput(String command) {
	def result = sh(
			script: "${command}",
			returnStdout: true)
	return result
}

Boolean containsCaseInsensitive(String s, String[] l) {
	for (String string : l) {
		if (string.equalsIgnoreCase(s)) {
			return true
		}
	}
	return false
}

List<String> extractSubListUsingRegex(String regex, List<String> l) {
	List<String> subList = []
	for (String string : l) {
		if (string.matches(regex)) {
			subList.add(string)
		}
	}
	return subList
}