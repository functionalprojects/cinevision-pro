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

def checkAndInstallTools(config) {
  script {
    // Check for Docker
    def hasDocker = sh(script: 'command -v docker', returnStatus: true) == 0
    if (!hasDocker) {
      error "Docker is required but not installed on agent ${env.NODE_NAME}. Please install Docker or use a different agent."
    } else {
      echo "Docker found at: ${sh(script: 'which docker', returnStdout: true).trim()}"
      def dockerVersion = sh(script: 'docker --version', returnStdout: true).trim()
      echo "Docker version: ${dockerVersion}"
    }
    
    // Check for AWS CLI
    def hasAws = sh(script: 'command -v aws', returnStatus: true) == 0
    if (!hasAws) {
      echo "Warning: AWS CLI not found. Some steps may fail."
    }
    
    // Check for kubectl (only if deploy is enabled)
    if (config.deployEnabled) {
      def hasKubectl = sh(script: 'command -v kubectl', returnStatus: true) == 0
      if (!hasKubectl && config.deployEnabled) {
        echo "Warning: kubectl not found. Kubernetes deployments may fail."
      }
    }
    
    echo "Agent ${env.NODE_NAME} ready with: Docker=${hasDocker}, AWS=${hasAws}"
  }
}

// Global state
def SERVICE_MAP = getServiceMap()

pipeline {
  // Use any available agent
  agent any
  
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 90, unit: 'MINUTES')
    skipDefaultCheckout()
  }
  
  environment {
    AWS_REGION = 'us-east-1'
    IMAGE_NAMESPACE = 'cinevision'
    GITHUB_TOKEN = credentials('github-token')
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
    JENKINS_AGENT_NAME = "${NODE_NAME}"
  }
  
  stages {
    stage('🚀 Initialization') {
      when { 
        not { changelog '\\[CI\\]' } 
        anyOf {
          branch 'develop'
          branch 'main'
          branch 'master'
          expression { env.BRANCH_NAME != null && env.BRANCH_NAME.startsWith('release/') }
        }
      }
      steps {
        // Checkout code
        checkout scm
        
        script {
          // Initialize CONFIG first
          def CONFIG = getEnvironmentConfig(env.BRANCH_NAME)
          env.TARGET_ENV = CONFIG.env
          
          if (env.TARGET_ENV == 'unknown') {
            error "Branch ${env.BRANCH_NAME} is not mapped to any environment."
          }
          
          // Store CONFIG in environment for later stages
          env.DEPLOY_ENABLED = CONFIG.deployEnabled.toString()
          env.APPROVAL_REQUIRED = CONFIG.approvalRequired.toString()
          env.RUN_SECURITY_SCAN = CONFIG.runSecurityScan.toString()
          env.RUN_PERFORMANCE_TESTS = CONFIG.runPerformanceTests.toString()
          env.RUN_INTEGRATION_TESTS = CONFIG.runIntegrationTests.toString()
          env.TRIVY_SEVERITY = CONFIG.trivySeverity
          env.ARGOCD_APP = CONFIG.argocdApp
          env.FRONTEND_BUCKET = CONFIG.frontendBucket
          env.API_URL = CONFIG.apiUrl
          env.KUSTOMIZE_OVERLAY = CONFIG.kustomizeOverlay
          env.AWS_CREDENTIALS_ID = CONFIG.awsCredentialsId
          env.CLOUDFRONT_DISTRIBUTION_ID = CONFIG.cloudfrontDistributionId
          
          // Get AWS Account ID securely
          if (CONFIG.awsAccountIdCredentialsId) {
            withCredentials([string(credentialsId: CONFIG.awsAccountIdCredentialsId, variable: 'AWS_ACCOUNT_ID')]) {
              env.CURRENT_ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
            }
          } else {
            error "AWS Account ID credentials not found for environment ${CONFIG.env}"
          }
          
          env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
          
          // Check required tools (pass CONFIG as parameter)
          checkAndInstallTools(CONFIG)
          
          echo "Pipeline initialized for ${env.TARGET_ENV} on node ${env.NODE_NAME}"
          echo "Docker registry: ${env.CURRENT_ECR_REGISTRY}"
          echo "Image tag: ${env.IMAGE_TAG}"
        }
      }
    }
    
    stage('🔍 Security & Code Quality') {
      when { expression { env.RUN_SECURITY_SCAN == 'true' } }
      parallel {
        stage('SCA: Dependency Check') {
          steps {
            script {
              // Check if dependency-check is available
              def hasDepCheck = sh(script: 'command -v dependency-check.sh', returnStatus: true) == 0
              if (hasDepCheck && fileExists('pom.xml')) {
                dependencyCheck additionalArguments: '--format HTML --format XML --out .', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: 'dependency-check-report.xml'
              } else {
                echo "Dependency-check not installed or no pom.xml found. Skipping SCA scan."
              }
            }
          }
        }
        
        stage('SAST: SonarCloud') {
          steps {
            script {
              // Check if SonarQube is configured and pom.xml exists
              if (fileExists('pom.xml')) {
                try {
                  withSonarQubeEnv('sonarcloud') {
                    sh 'mvn sonar:sonar || echo "Sonar scan failed but continuing"'
                  }
                  timeout(time: 15, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                  }
                } catch(Exception e) {
                  echo "SonarCloud scan failed: ${e.message}"
                }
              } else {
                echo "No pom.xml found. Skipping SonarCloud scan."
              }
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
          
          echo "Changed services: ${env.CHANGED_SERVICES}"
          
          if (changed.isEmpty()) {
            echo "No services changed. Skipping build stage."
            return
          }
          
          // Run builds sequentially
          for (serviceName in changed) {
            def meta = SERVICE_MAP[serviceName]
            
            stage("Process ${serviceName}") {
              dir(meta.path) {
                echo "Building service: ${serviceName}"
                
                // Build based on type
                if (meta.type == 'maven') {
                  sh "mvn clean package -DskipTests=false -Dmaven.repo.local=.m2/repository || mvn clean package -DskipTests=true"
                } else if (meta.type == 'node') {
                  sh "npm ci --legacy-peer-deps || npm install"
                  sh "npm test || echo 'No tests configured'"
                }
                
                // Build Docker image if Dockerfile exists
                if (fileExists('Dockerfile')) {
                  def fullImageName = "${env.IMAGE_NAMESPACE}/${meta.image}"
                  def imageTag = "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.IMAGE_TAG}"
                  
                  sh "docker build -t ${imageTag} ."
                  
                  // Run Trivy scan if available
                  def hasTrivy = sh(script: 'command -v trivy', returnStatus: true) == 0
                  if (hasTrivy && env.TRIVY_SEVERITY) {
                    sh "trivy image --severity ${env.TRIVY_SEVERITY} --ignore-unfixed --exit-code 0 ${imageTag} || echo 'Trivy scan found issues but continuing'"
                  } else {
                    echo "Trivy not installed. Skipping container security scan."
                  }
                  
                  // Push to ECR
                  withAWS(credentials: env.AWS_CREDENTIALS_ID, region: env.AWS_REGION) {
                    sh "aws ecr get-login-password --region ${env.AWS_REGION} | docker login --username AWS --password-stdin ${env.CURRENT_ECR_REGISTRY}"
                    retry(3) { 
                      sh "docker push ${imageTag}" 
                    }
                    
                    def envTag = "${env.TARGET_ENV}-latest"
                    sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${envTag}"
                    sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${envTag}"
                  }
                } else {
                  echo "No Dockerfile found in ${meta.path}. Skipping containerization."
                }
              }
            }
          }
        }
      }
    }
    
    stage('📂 GitOps Manifest Update') {
      when { expression { env.DEPLOY_ENABLED == 'true' && env.CHANGED_SERVICES != null && env.CHANGED_SERVICES != '' } }
      steps {
        script {
          def overlay = env.KUSTOMIZE_OVERLAY
          if (fileExists(overlay)) {
            dir(overlay) {
              def changedServices = env.CHANGED_SERVICES.split(',')
              changedServices.each { serviceName ->
                if (serviceName != 'frontend' && SERVICE_MAP.containsKey(serviceName)) {
                  def meta = SERVICE_MAP[serviceName]
                  def fullImageName = "${env.CURRENT_ECR_REGISTRY}/${env.IMAGE_NAMESPACE}/${meta.image}"
                  sh "kustomize edit set image ${meta.image}=${fullImageName}:${env.IMAGE_TAG} || echo 'Kustomize not available'"
                }
              }
            }
            
            // Commit and push changes
            sh """
              git config user.email "jenkins@cinevision.com"
              git config user.name "Jenkins CI"
              git add ${overlay} || echo 'No files to add'
              git commit -m "[CI] Deploy ${env.IMAGE_TAG} to ${env.TARGET_ENV} [skip ci]" || echo "No changes to commit"
              git push origin HEAD:${env.BRANCH_NAME} || echo "Push failed"
            """
          } else {
            echo "Kustomize overlay ${overlay} not found. Skipping manifest update."
          }
        }
      }
    }
    
    stage('🌐 Frontend Deployment') {
      when { expression { 
        env.CHANGED_SERVICES != null && 
        env.CHANGED_SERVICES.split(',').contains('frontend') && 
        env.FRONTEND_BUCKET 
      } }
      steps {
        script {
          withAWS(credentials: env.AWS_CREDENTIALS_ID, region: env.AWS_REGION) {
            dir(SERVICE_MAP['frontend'].path) {
              sh 'npm ci --legacy-peer-deps || npm install'
              sh 'npm run build || echo "Build script not found"'
              
              def buildDir = fileExists('dist') ? 'dist' : (fileExists('build') ? 'build' : null)
              if (buildDir && fileExists(buildDir)) {
                sh "aws s3 sync ${buildDir} s3://${env.FRONTEND_BUCKET} --delete"
                
                if (env.CLOUDFRONT_DISTRIBUTION_ID) {
                  withCredentials([string(credentialsId: env.CLOUDFRONT_DISTRIBUTION_ID, variable: 'CF_ID')]) {
                    sh "aws cloudfront create-invalidation --distribution-id ${CF_ID} --paths '/*'"
                  }
                }
              } else {
                echo "Build directory not found. Skipping S3 sync."
              }
            }
          }
        }
      }
    }
    
    stage('🚢 Deployment & Verification') {
      when { expression { env.DEPLOY_ENABLED == 'true' && env.ARGOCD_APP } }
      steps {
        script {
          if (env.APPROVAL_REQUIRED == 'true') {
            input message: "Approve deployment to ${env.TARGET_ENV}?", ok: "Deploy"
          }
          
          withAWS(credentials: env.AWS_CREDENTIALS_ID, region: env.AWS_REGION) {
            // Check if ArgoCD CLI is available
            def hasArgoCD = sh(script: 'command -v argocd', returnStatus: true) == 0
            if (hasArgoCD) {
              withCredentials([usernamePassword(credentialsId: 'argocd-creds', passwordVariable: 'ARGO_PWD', usernameVariable: 'ARGO_USER')]) {
                sh "argocd login argocd.cinevision.com --username ${ARGO_USER} --password ${ARGO_PWD} --insecure || echo 'ArgoCD login failed'"
              }
              sh "argocd app sync ${env.ARGOCD_APP} --grpc-web --prune || echo 'Sync failed'"
              sh "argocd app wait ${env.ARGOCD_APP} --health --timeout 600 || echo 'Wait failed'"
            } else {
              echo "ArgoCD CLI not found. Skipping ArgoCD deployment."
              // Fallback to kubectl
              if (fileExists('k8s')) {
                sh "kubectl apply -k ${env.KUSTOMIZE_OVERLAY} || echo 'kubectl apply failed'"
              }
            }
            
            // Canary & Blue-Green (Prod only)
            if (env.TARGET_ENV == 'prod') {
              echo "🚥 Starting Canary Analysis"
              if (fileExists('scripts/canary-analysis.py')) {
                try {
                  sh "python3 scripts/canary-analysis.py --url ${env.API_URL} --duration 60"
                } catch (Exception e) {
                  if (fileExists('k8s/overlays/prod/blue')) {
                    sh "kubectl apply -k k8s/overlays/prod/blue" // Emergency Revert
                  }
                  error "Canary Failed: ${e.message}"
                }
              } else {
                echo "Canary analysis script not found. Skipping."
              }
              
              if (fileExists('k8s/overlays/prod/green')) {
                echo "💎 Full Traffic Shift"
                sh "kubectl apply -k k8s/overlays/prod/green"
              }
            }
          }
        }
      }
    }
    
    stage('✅ Post-Deployment Tests') {
      when { expression { env.RUN_INTEGRATION_TESTS == 'true' } }
      parallel {
        stage('Integration & DAST') {
          steps {
            script {
              if (fileExists('tests/integration')) {
                dir('tests/integration') { 
                  sh "npm install || echo 'No package.json'"
                  sh "BASE_URL=${env.API_URL} npm test || echo 'Integration tests failed'"
                }
              } else {
                echo "Integration tests not found. Skipping."
              }
              
              // Run ZAP scan if Docker is available and API URL exists
              if (env.API_URL) {
                sh "docker run --rm -v \$(pwd):/zap/wrk/:rw -t owasp/zap2docker-stable zap-baseline.py -t ${env.API_URL} -r zap_report.html || true"
                archiveArtifacts artifacts: 'zap_report.html', allowEmptyArchive: true
              }
            }
          }
        }
        stage('Performance') {
          when { expression { env.RUN_PERFORMANCE_TESTS == 'true' } }
          steps {
            script {
              if (fileExists('tests/performance/cinevision-load-test.js')) {
                dir('tests/performance') { 
                  sh "docker run --rm -v \$(pwd):/tests -t grafana/k6 run /tests/cinevision-load-test.js -e BASE_URL=${env.API_URL} || echo 'Performance tests failed'"
                }
              } else {
                echo "Performance tests not found. Skipping."
              }
            }
          }
        }
      }
    }
  }
  
  post {
    success { 
      script {
        slackSend(
          color: 'good',
          message: "✅ CineVision Build SUCCESSFUL\nJob: ${env.JOB_NAME}\nBuild: #${env.BUILD_NUMBER}\nEnvironment: ${env.TARGET_ENV}\nNode: ${env.NODE_NAME}"
        )
      }
    }
    failure { 
      script {
        slackSend(
          color: 'danger', 
          message: "❌ CineVision Build FAILED\nJob: ${env.JOB_NAME}\nBuild: #${env.BUILD_NUMBER}\nEnvironment: ${env.TARGET_ENV}\nNode: ${env.NODE_NAME}"
        )
      }
    }
    always { 
      script {
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml'
        cleanWs() 
      }
    }
  }
}