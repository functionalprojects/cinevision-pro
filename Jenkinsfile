// ============================================
// CINEVISION ENTERPRISE CI/CD PIPELINE v2
// Production-Ready DevSecOps Pipeline
// ============================================

import groovy.json.JsonSlurperClassic

// ============================================
// GLOBAL CONFIG
// ============================================

def getServiceMap() {
    return [
        'api-gateway': [
            path      : 'services/api-gateway',
            type      : 'maven',
            image     : 'api-gateway',
            sonarKey  : 'cinevision_api-gateway1',
            port      : 8080
        ],

        'user-service': [
            path      : 'services/userService',
            type      : 'maven',
            image     : 'user-service',
            sonarKey  : 'cinevision_user-service1',
            port      : 8081
        ],

        'movie-service': [
            path      : 'services/movieService',
            type      : 'maven',
            image     : 'movie-service',
            sonarKey  : 'cinevision_movie-service1',
            port      : 8082
        ],

        'email-service': [
            path      : 'services/emailService',
            type      : 'maven',
            image     : 'email-service',
            sonarKey  : 'cinevision_email-service1',
            port      : 8083
        ],

        'eureka-server': [
            path      : 'services/eureka-server',
            type      : 'maven',
            image     : 'eureka-server',
            sonarKey  : 'cinevision_eureka-server1',
            port      : 8761
        ],

        'frontend': [
            path      : 'services/frontend',
            type      : 'node',
            image     : 'frontend',
            sonarKey  : null,
            port      : 3000
        ]
    ]
}

// ============================================
// ENVIRONMENT CONFIG
// ============================================

def getEnvironmentConfig(String branch) {

    if (branch == 'develop') {
        return [
            env                : 'dev',
            namespace          : 'cinevision-dev',
            awsCredentialsId   : 'ecr-eks',
            awsAccountId       : '211026994790',
            argocdApp          : 'cinevision-dev',
            deployEnabled      : true,
            approvalRequired   : false,
            apiUrl             : 'https://dev-api.cinevisionca.link',
            overlay            : 'k8s/overlays/dev',
            frontendBucket     : 'dev-cinevision-dev-frontend'
        ]
    }

    if (branch.startsWith('release/')) {
        return [
            env                : 'staging',
            namespace          : 'cinevision-staging',
            awsCredentialsId   : 'aws-staging-credentials',
            awsAccountId       : '211026994790',
            argocdApp          : 'cinevision-staging',
            deployEnabled      : true,
            approvalRequired   : true,
            apiUrl             : 'https://staging-api.cinevision.com',
            overlay            : 'k8s/overlays/staging',
            frontendBucket     : 'staging-cinevision-staging-frontend'
        ]
    }

    if (branch == 'main' || branch == 'master') {
        return [
            env                : 'prod',
            namespace          : 'cinevision-prod',
            awsCredentialsId   : 'aws-prod-credentials',
            awsAccountId       : '211026994790',
            argocdApp          : 'cinevision-prod-green',
            deployEnabled      : true,
            approvalRequired   : true,
            apiUrl             : 'https://api.cinevision.com',
            overlay            : 'k8s/overlays/prod/green',
            frontendBucket     : 'prod-cinevision-prod-frontend'
        ]
    }

    return [env: 'unknown']
}

// ============================================
// HELPERS
// ============================================

def retryShell(command, retries = 3) {
    retry(retries) {
        sh """
            set -eo pipefail
            ${command}
        """
    }
}

def awsWrap(credentialsId, Closure body) {
    withCredentials([
        [
            $class: 'AmazonWebServicesCredentialsBinding',
            credentialsId: credentialsId,
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        ]
    ]) {
        body()
    }
}

def discoverServices(serviceMap) {

    def discovered = [:]

    serviceMap.each { name, meta ->
        if (fileExists(meta.path)) {
            discovered[name] = meta
            echo "Discovered service: ${name}"
        }
    }

    return discovered
}

def detectChangedServices(services) {

    def changedFiles = sh(
        script: """
            git diff --name-only origin/${env.CHANGE_TARGET ?: 'develop'}...HEAD
        """,
        returnStdout: true
    ).trim().split("\n")

    def changed = []

    services.each { serviceName, meta ->
        if (changedFiles.any { it.startsWith("${meta.path}/") }) {
            changed << serviceName
        }
    }

    if (changed.isEmpty()) {
        changed = services.keySet() as List
    }

    return changed.unique()
}

def loginECR(region, registry, credentialsId) {

    awsWrap(credentialsId) {

        retryShell("""
            aws ecr get-login-password \
              --region ${region} \
            | docker login \
              --username AWS \
              --password-stdin ${registry}
        """)
    }
}

def ensureECRRepository(serviceName, region, credentialsId) {

    awsWrap(credentialsId) {

        sh """
            aws ecr describe-repositories \
              --repository-names cinevision/${serviceName} \
              --region ${region} >/dev/null 2>&1 \
            || \
            aws ecr create-repository \
              --repository-name cinevision/${serviceName} \
              --image-scanning-configuration scanOnPush=true \
              --region ${region}
        """
    }
}

def buildMavenService(meta) {

    dir(meta.path) {

        retryShell("""
            mvn -B clean package \
                -DskipTests \
                -Dmaven.repo.local=.m2/repository
        """)
    }
}

def buildNodeService(meta) {

    dir(meta.path) {

        retryShell("""
            npm ci --prefer-offline --no-audit
        """)

        retryShell("""
            CI=false npm run build
        """)
    }
}

def buildDockerImage(serviceName, meta, fullImage) {

    dir(meta.path) {

        retryShell("""
            docker buildx build \
              --platform linux/amd64 \
              -t ${fullImage} \
              -t ${serviceName}:latest \
              .
        """)
    }
}

def pushDockerImage(fullImage) {

    retryShell("""
        docker push ${fullImage}
    """)
}

def updateKustomize(serviceName, image, overlay) {

    dir(overlay) {

        sh """
            kustomize edit set image \
              cinevision/${serviceName}=${image}
        """
    }
}

def commitGitOpsChanges() {

    sh """
        git config user.email "jenkins@cinevision.com"
        git config user.name "Jenkins CI"

        git add .

        git diff --cached --quiet || \
        git commit -m "[CI] Update deployment manifests"

        git push origin HEAD:${env.BRANCH_NAME}
    """
}

pipeline {

    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 120, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        durabilityHint('MAX_SURVIVABILITY')
        skipDefaultCheckout()
    }

    environment {
        AWS_REGION = 'us-east-1'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm

                sh """
                    git fetch --all --prune
                """
            }
        }

        stage('Initialize') {

            steps {

                script {

                    def cfg = getEnvironmentConfig(env.BRANCH_NAME)

                    if (cfg.env == 'unknown') {
                        error "Unsupported branch: ${env.BRANCH_NAME}"
                    }

                    env.TARGET_ENV = cfg.env
                    env.NAMESPACE = cfg.namespace
                    env.AWS_CREDENTIALS_ID = cfg.awsCredentialsId
                    env.AWS_ACCOUNT_ID = cfg.awsAccountId
                    env.ARGOCD_APP = cfg.argocdApp
                    env.DEPLOY_ENABLED = cfg.deployEnabled.toString()
                    env.OVERLAY = cfg.overlay
                    env.FRONTEND_BUCKET = cfg.frontendBucket

                    env.ECR_REGISTRY =
                        "${cfg.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

                    env.GIT_SHA =
                        sh(
                            script: "git rev-parse --short=8 HEAD",
                            returnStdout: true
                        ).trim()

                    env.IMAGE_TAG =
                        "${env.BUILD_NUMBER}-${env.GIT_SHA}"

                    SERVICES =
                        discoverServices(getServiceMap())

                    CHANGED_SERVICES =
                        detectChangedServices(SERVICES)

                    echo "Changed services: ${CHANGED_SERVICES}"
                }
            }
        }

        stage('Build Services') {

            steps {

                script {

                    def parallelBuilds = [:]

                    CHANGED_SERVICES.each { serviceName ->

                        def meta = SERVICES[serviceName]

                        parallelBuilds[serviceName] = {

                            stage("Build ${serviceName}") {

                                if (meta.type == 'maven') {
                                    buildMavenService(meta)
                                }

                                if (meta.type == 'node') {
                                    buildNodeService(meta)
                                }

                                def image =
                                    "${env.ECR_REGISTRY}/cinevision/${serviceName}:${env.IMAGE_TAG}"

                                buildDockerImage(serviceName, meta, image)
                            }
                        }
                    }

                    parallel parallelBuilds
                }
            }
        }

        stage('Security Scan') {

            steps {

                script {

                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {

                        sh """
                            trivy image \
                              --severity HIGH,CRITICAL \
                              --exit-code 0 \
                              ${env.ECR_REGISTRY}/cinevision/api-gateway:${env.IMAGE_TAG}
                        """
                    }
                }
            }
        }

        stage('Push Images') {

            steps {

                script {

                    loginECR(
                        env.AWS_REGION,
                        env.ECR_REGISTRY,
                        env.AWS_CREDENTIALS_ID
                    )

                    CHANGED_SERVICES.each { serviceName ->

                        ensureECRRepository(
                            serviceName,
                            env.AWS_REGION,
                            env.AWS_CREDENTIALS_ID
                        )

                        def image =
                            "${env.ECR_REGISTRY}/cinevision/${serviceName}:${env.IMAGE_TAG}"

                        pushDockerImage(image)
                    }
                }
            }
        }

        stage('Update GitOps') {

            when {
                expression {
                    env.DEPLOY_ENABLED == 'true'
                }
            }

            steps {

                script {

                    CHANGED_SERVICES.each { serviceName ->

                        def image =
                            "${env.ECR_REGISTRY}/cinevision/${serviceName}:${env.IMAGE_TAG}"

                        updateKustomize(
                            serviceName,
                            image,
                            env.OVERLAY
                        )
                    }

                    commitGitOpsChanges()
                }
            }
        }

        stage('ArgoCD Sync') {

            when {
                expression {
                    env.DEPLOY_ENABLED == 'true'
                }
            }

            steps {

                script {

                    sh """
                        argocd app sync ${env.ARGOCD_APP} --grpc-web
                    """

                    sh """
                        argocd app wait ${env.ARGOCD_APP} \
                          --health \
                          --sync \
                          --timeout 600 \
                          --grpc-web
                    """
                }
            }
        }

        stage('Frontend Deploy') {

            when {
                expression {
                    CHANGED_SERVICES.contains('frontend')
                }
            }

            steps {

                script {

                    awsWrap(env.AWS_CREDENTIALS_ID) {

                        dir('services/frontend/build') {

                            retryShell("""
                                aws s3 sync . \
                                  s3://${env.FRONTEND_BUCKET}/ \
                                  --delete
                            """)
                        }
                    }
                }
            }
        }

        stage('Smoke Tests') {

            steps {

                script {

                    sh """
                        kubectl rollout status deployment/api-gateway \
                          -n ${env.NAMESPACE} \
                          --timeout=300s
                    """

                    sh """
                        curl -k --fail \
                          ${getEnvironmentConfig(env.BRANCH_NAME).apiUrl}/actuator/health
                    """
                }
            }
        }
    }

    post {

        success {

            echo "Pipeline completed successfully"
        }

        failure {

            echo "Pipeline failed"
        }

        always {

            junit(
                allowEmptyResults: true,
                testResults: '**/surefire-reports/*.xml'
            )

            sh """
                docker system prune -af || true
            """

            cleanWs(
                deleteDirs: true,
                disableDeferredWipeout: true
            )
        }
    }
}