// ============================================
// CINEVISION ENTERPRISE CI/CD PIPELINE
// Industrial-grade automation with DevSecOps
// ============================================

import groovy.json.JsonOutput

// Global Configuration
def getServiceMap() {
  return [
    'api-gateway'  : [path: 'services/api-gateway',   type: 'maven', image: 'api-gateway'],
    'user-service' : [path: 'services/userService',   type: 'maven', image: 'user-service'],
    'movie-service': [path: 'services/movieService',  type: 'maven', image: 'movie-service'],
    'email-service': [path: 'services/emailService',  type: 'maven', image: 'email-service'],
    'eureka-server': [path: 'services/eureka-server', type: 'maven', image: 'eureka-server'],
    'frontend'     : [path: 'services/frontend',      type: 'node',  image: 'frontend']
  ]
}

@NonCPS
def getEnvironmentConfig(String branch) {
  if (branch == 'main' || branch == 'master' || branch.startsWith('hotfix/')) {
    return [
      env: 'prod',
      awsAccountIdCredentialsId: 'PROD_AWS_ACCOUNT_ID',
      awsCredentialsId: 'aws-prod-credentials',
      argocdApp: 'cinevision-prod-green',
      frontendBucket: 'prod-cinevision-prod-frontend',
      cloudfrontDistributionId: 'PROD_CLOUDFRONT_DISTRIBUTION_ID',
      posterCloudfrontId: 'PROD_POSTER_CLOUDFRONT_ID',
      archiveCloudfrontId: 'PROD_ARCHIVE_CLOUDFRONT_ID',
      apiUrl: 'https://api.cinevision.com',
      kustomizeOverlay: 'k8s/overlays/prod/green',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      trivySeverity: 'HIGH,CRITICAL'
    ]
  }
  
  if (branch.startsWith('release/')) {
    return [
      env: 'staging',
      awsAccountIdCredentialsId: 'STAGING_AWS_ACCOUNT_ID',
      awsCredentialsId: 'aws-staging-credentials',
      argocdApp: 'cinevision-staging',
      frontendBucket: 'staging-cinevision-staging-frontend',
      cloudfrontDistributionId: 'STAGING_CLOUDFRONT_DISTRIBUTION_ID',
      posterCloudfrontId: 'STAGING_POSTER_CLOUDFRONT_ID',
      archiveCloudfrontId: 'STAGING_ARCHIVE_CLOUDFRONT_ID',
      apiUrl: 'https://staging-api.cinevision.com',
      kustomizeOverlay: 'k8s/overlays/staging',
      deployEnabled: true,
      approvalRequired: true,
      runSecurityScan: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      trivySeverity: 'HIGH,CRITICAL'
    ]
  }
  
  if (branch == 'develop') {
    return [
      env: 'dev',
      awsAccountIdCredentialsId: 'DEV_AWS_ACCOUNT_ID',
      awsCredentialsId: 'ecr-eks',
      argocdApp: 'cinevision-dev',
      frontendBucket: 'dev-cinevision-dev-frontend',
      cloudfrontDistributionId: 'DEV_CLOUDFRONT_DISTRIBUTION_ID',
      posterCloudfrontId: 'DEV_POSTER_CLOUDFRONT_ID',
      archiveCloudfrontId: 'DEV_ARCHIVE_CLOUDFRONT_ID',
      apiUrl: 'https://dev-api.cinevisionca.link',
      kustomizeOverlay: 'k8s/overlays/dev',
      deployEnabled: true,
      approvalRequired: false,
      runSecurityScan: true,
      runPerformanceTests: false,
      runIntegrationTests: true,
      trivySeverity: 'CRITICAL'
    ]
  }
  
  return [env: 'unknown', deployEnabled: false]
}

def detectChangedServices(serviceMap) {
  def changedFiles = sh(
    script: """
        if git rev-parse HEAD~1 >/dev/null 2>&1; then
            git diff --name-only HEAD~1 HEAD
        else
            git ls-files
        fi
    """,
    returnStdout: true
  ).trim().split('\n') as List
  
  def changed = []
  serviceMap.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
    }
  }
  
  if (changed.isEmpty()) {
    echo "No specific changes - targeting all services"
    changed = serviceMap.keySet() as List
  }
  return changed.unique()
}

// Global state (Non-serialized)
def CONFIG = [:]
def SERVICE_MAP = getServiceMap()

pipeline {
  agent { label 'devsecops' } // Required industrial practice
  
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 90, unit: 'MINUTES')
  }
  
  environment {
    AWS_REGION = 'us-east-1'
    IMAGE_NAMESPACE = 'cinevision'
    GITHUB_TOKEN = credentials('github-token')
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
  }
  
  stages {
    stage('🚀 Initialization') {
      when { 
        not { changelog '\\[CI\\]' } 
        anyOf {
          branch 'develop'
          branch 'main'
          expression { env.BRANCH_NAME.startsWith('release/') }
        }
      }
      steps {
        script {
          CONFIG = getEnvironmentConfig(env.BRANCH_NAME)
          env.TARGET_ENV = CONFIG.env
          
          if (env.TARGET_ENV == 'unknown') {
            error "Branch ${env.BRANCH_NAME} is not mapped to any environment."
          }
          
          withCredentials([string(credentialsId: CONFIG.awsAccountIdCredentialsId, variable: 'AWS_ACCOUNT_ID')]) {
            env.CURRENT_ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
          }
          
          env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
          
          echo "Pipeline initialized for ${env.TARGET_ENV}"
        }
      }
    }
    
    stage('🔍 Security & Code Quality') {
      parallel {
        stage('SCA: Dependency Check') {
          steps {
            dependencyCheck additionalArguments: '--format HTML --format XML --out .', odcInstallation: 'DP-Check'
            dependencyCheckPublisher pattern: 'dependency-check-report.xml'
          }
        }
        
        stage('SAST: SonarCloud') {
          steps {
            withSonarQubeEnv('sonarcloud') {
              sh 'mvn sonar:sonar'
            }
            timeout(time: 15, unit: 'MINUTES') {
              waitForQualityGate abortPipeline: true
            }
          }
        }
      }
    }
    
    stage('📦 Build & Containerize') {
      steps {
        script {
          def changed = detectChangedServices(SERVICE_MAP)
          env.CHANGED_SERVICES = changed.join(',')
          
          def branches = [:]
          changed.each { serviceName ->
            def meta = SERVICE_MAP[serviceName]
            branches[serviceName] = {
              node('devsecops') { // Independent node for true parallelism
                stage("Process ${serviceName}") {
                  dir(meta.path) {
                    checkout scm
                    
                    // Build
                    if (meta.type == 'maven') {
                      sh "mvn clean package -DskipTests=false -Dmaven.repo.local=.m2/repository"
                    } else {
                      sh "npm ci && npm test"
                    }
                    
                    // Docker
                    def fullImageName = "${env.IMAGE_NAMESPACE}/${meta.image}"
                    def imageTag = "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.IMAGE_TAG}"
                    
                    sh "docker build -t ${imageTag} ."
                    sh "trivy image --severity ${CONFIG.trivySeverity} --ignore-unfixed --exit-code 1 ${imageTag}"
                    
                    withAWS(credentials: CONFIG.awsCredentialsId, region: env.AWS_REGION) {
                      sh "aws ecr get-login-password --region ${env.AWS_REGION} | docker login --username AWS --password-stdin ${env.CURRENT_ECR_REGISTRY}"
                      retry(3) { sh "docker push ${imageTag}" }
                      
                      def envTag = "${env.TARGET_ENV}-latest"
                      sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${envTag}"
                      sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${envTag}"
                    }
                  }
                }
              }
            }
          }
          parallel branches
        }
      }
    }
    
    stage('📂 GitOps Manifest Update') {
      when { expression { CONFIG.deployEnabled } }
      steps {
        script {
          def overlay = CONFIG.kustomizeOverlay
          dir(overlay) {
            env.CHANGED_SERVICES.split(',').each { serviceName ->
              if (serviceName != 'frontend') {
                def meta = SERVICE_MAP[serviceName]
                def fullImageName = "${env.CURRENT_ECR_REGISTRY}/${env.IMAGE_NAMESPACE}/${meta.image}"
                sh "kustomize edit set image ${meta.image}=${fullImageName}:${env.IMAGE_TAG}"
              }
            }
          }
          
          sh """
            git config user.email "jenkins@cinevision.com"
            git config user.name "Jenkins CI"
            git remote set-url origin https://${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git
            git add ${overlay}
            git commit -m "[CI] Deploy ${env.IMAGE_TAG} to ${env.TARGET_ENV} [skip ci]" || echo "No changes"
            git push origin HEAD:${env.BRANCH_NAME}
          """
        }
      }
    }
    
    stage('🌐 Frontend Deployment') {
      when { expression { env.CHANGED_SERVICES.split(',').contains('frontend') } }
      steps {
        script {
          withAWS(credentials: CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            dir(SERVICE_MAP['frontend'].path) {
              sh 'npm ci && npm run build'
              def buildDir = 'dist' // Configurable for Vite
              sh "aws s3 sync ${buildDir} s3://${CONFIG.frontendBucket} --delete"
              
              withCredentials([string(credentialsId: CONFIG.cloudfrontDistributionId, variable: 'CF_ID')]) {
                sh "aws cloudfront create-invalidation --distribution-id ${CF_ID} --paths '/*'"
              }
            }
          }
        }
      }
    }
    
    stage('🚢 Deployment & Verification') {
      when { expression { CONFIG.deployEnabled } }
      steps {
        script {
          if (CONFIG.approvalRequired) {
            input message: "Approve deployment to ${env.TARGET_ENV}?", ok: "Deploy"
          }
          
          withAWS(credentials: CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            // ArgoCD Auth & Sync
            withCredentials([usernamePassword(credentialsId: 'argocd-creds', passwordVariable: 'ARGO_PWD', usernameVariable: 'ARGO_USER')]) {
              sh "argocd login argocd.cinevision.com --username ${ARGO_USER} --password ${ARGO_PWD} --insecure"
            }
            sh "argocd app sync ${CONFIG.argocdApp} --grpc-web --prune"
            sh "argocd app wait ${CONFIG.argocdApp} --health --timeout 600"
            
            // Canary & Blue-Green (Prod only)
            if (env.TARGET_ENV == 'prod') {
              echo "🚥 Starting Canary Analysis"
              try {
                sh "python3 scripts/canary-analysis.py --url ${CONFIG.apiUrl} --duration 60"
              } catch (Exception e) {
                sh "kubectl apply -k k8s/overlays/prod/blue" // Emergency Revert
                error "Canary Failed: ${e.message}"
              }
              
              echo "💎 Full Traffic Shift"
              sh "kubectl apply -k k8s/overlays/prod/green"
            }
          }
        }
      }
    }
    
    stage('✅ Post-Deployment Tests') {
      parallel {
        stage('Integration & DAST') {
          steps {
            script {
              dir('tests/integration') { sh "npm install && BASE_URL=${CONFIG.apiUrl} npm test" }
              sh "docker run --rm -v \$(pwd):/zap/wrk/:rw -t owasp/zap2docker-stable zap-baseline.py -t ${CONFIG.apiUrl} -r zap_report.html || true"
              archiveArtifacts artifacts: 'zap_report.html', allowEmptyArchive: true
            }
          }
        }
        stage('Performance') {
          when { expression { CONFIG.runPerformanceTests } }
          steps {
            dir('tests/performance') { sh "docker run --rm -v \$(pwd):/tests -t grafana/k6 run /tests/cinevision-load-test.js -e BASE_URL=${CONFIG.apiUrl}" }
          }
        }
      }
    }
  }
  
  post {
    success { sendSlackNotification('SUCCESSFUL') }
    failure { sendSlackNotification('FAILED') }
    always { 
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml'
      cleanWs() 
    }
  }
}

// ============================================
// Helper Functions
// ============================================

def sendSlackNotification(String buildStatus) {
    def colorCode = buildStatus == 'SUCCESSFUL' ? 'good' : (buildStatus == 'FAILED' ? 'danger' : 'warning')
    def emoji = buildStatus == 'SUCCESSFUL' ? '✅' : '❌'
    
    slackSend(
        color: colorCode,
        message: """
${emoji} *CineVision Build ${buildStatus}*
*Job:* ${env.JOB_NAME}
*Build:* <${env.BUILD_URL}|#${env.BUILD_NUMBER}>
*Environment:* ${env.TARGET_ENV}
*Commit:* <https://github.com/${env.GITHUB_REPO}/commit/${env.GIT_COMMIT}|${env.GIT_COMMIT_SHORT}>
*Services:* ${env.CHANGED_SERVICES}
        """.trim()
    )
}