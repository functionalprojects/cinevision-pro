// ============================================
// JENKINS PIPELINE FOR POST-MERGE CI/CD
// GitHub Actions handles all PR validation
// This pipeline runs AFTER code is merged to branches
// ============================================

def serviceMap = [
  'api-gateway'  : [path: 'api-gateway',   type: 'maven', image: 'api-gateway'],
  'user-service' : [path: 'userService',   type: 'maven', image: 'user-service'],
  'movie-service': [path: 'movieService',  type: 'maven', image: 'movie-service'],
  'email-service': [path: 'emailService',  type: 'maven', image: 'email-service'],
  'eureka-server': [path: 'eureka-server', type: 'maven', image: 'eureka-server'],
  'frontend'     : [path: 'frontend',      type: 'node',  image: 'frontend']
]

def detectChangedServices() {
  // Detect which services changed in the last commit
  def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim().split('\n') as List
  def changed = []
  
  serviceMap.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
    }
  }
  
  // If no specific services detected or this is the first build, build everything
  if (changed.isEmpty() || changedFiles.isEmpty()) {
    echo "No specific service changes detected or first build - building all services"
    changed = serviceMap.keySet() as List
  }
  
  return changed.unique()
}

def getEnvironmentConfig() {
  def branch = env.BRANCH_NAME
  
  // Production branches
  if (branch == 'main' || branch == 'master' || branch.startsWith('hotfix/')) {
    return [
      env: 'prod',
      awsAccountId: env.PROD_AWS_ACCOUNT_ID,
      awsCredentialsId: 'aws-prod-credentials',
      namespace: 'cinevision-prod',
      argocdApp: 'cinevision-prod-green',
      frontendBucket: env.PROD_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.PROD_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl: env.PROD_API_URL,
      kustomizeOverlay: env.PROD_GREEN_OVERLAY,
      deployEnabled: true,
      approvalRequired: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
  // Release/staging branches
  if (branch.startsWith('release/')) {
    return [
      env: 'staging',
      awsAccountId: env.STAGING_AWS_ACCOUNT_ID,
      awsCredentialsId: 'aws-staging-credentials',
      namespace: 'cinevision-staging',
      argocdApp: 'cinevision-staging',
      frontendBucket: env.STAGING_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.STAGING_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl: env.STAGING_API_URL,
      kustomizeOverlay: env.STAGING_OVERLAY,
      deployEnabled: true,
      approvalRequired: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
  // Development branch
  if (branch == 'develop') {
    return [
      env: 'dev',
      awsAccountId: env.DEV_AWS_ACCOUNT_ID,
      awsCredentialsId: 'ecr-eks',
      namespace: 'cinevision-dev',
      argocdApp: 'cinevision-dev',
      frontendBucket: env.DEV_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.DEV_CLOUDFRONT_DISTRIBUTION_ID,
      apiUrl: env.DEV_API_URL,
      kustomizeOverlay: env.DEV_OVERLAY,
      deployEnabled: true,
      approvalRequired: false,
      runPerformanceTests: false,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
  // Default - don't build or deploy
  return [
    env: 'unknown',
    deployEnabled: false,
    approvalRequired: false,
    runPerformanceTests: false,
    runIntegrationTests: false,
    buildImages: false
  ]
}

pipeline {
  // CRITICAL FIX: Agent must be defined at top level
  agent any
  
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 60, unit: 'MINUTES')
    skipDefaultCheckout()
  }
  
  environment {
    // AWS Configuration
    AWS_REGION = 'us-east-1'
    DR_AWS_REGION = 'us-west-2'
    
    // Account IDs (stored as Jenkins credentials)
    DEV_AWS_ACCOUNT_ID     = credentials('DEV_AWS_ACCOUNT_ID')
    STAGING_AWS_ACCOUNT_ID = credentials('STAGING_AWS_ACCOUNT_ID')
    PROD_AWS_ACCOUNT_ID    = credentials('PROD_AWS_ACCOUNT_ID')
    
    // Image namespace
    IMAGE_NAMESPACE = 'cinevision'
    
    // Kustomize overlay paths
    DEV_OVERLAY        = 'k8s/overlays/dev'
    STAGING_OVERLAY    = 'k8s/overlays/staging'
    PROD_BLUE_OVERLAY  = 'k8s/overlays/prod/blue'
    PROD_GREEN_OVERLAY = 'k8s/overlays/prod/green'
    
    // API URLs
    DEV_API_URL      = 'https://dev-api.cinevisionca.link'
    STAGING_API_URL  = 'https://staging-api.cinevision.com'
    PROD_API_URL     = 'https://api.cinevision.com'
    
    // Security configuration
    TRIVY_SEVERITY     = 'HIGH,CRITICAL'
    
    // Frontend S3 buckets
    DEV_FRONTEND_BUCKET      = 'cinevision-dev-frontend'
    STAGING_FRONTEND_BUCKET  = 'cinevision-staging-frontend'
    PROD_FRONTEND_BUCKET     = 'cinevision-prod-frontend'
    
    // CloudFront distribution IDs (stored as Jenkins credentials)
    DEV_CLOUDFRONT_DISTRIBUTION_ID      = credentials('DEV_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_CLOUDFRONT_DISTRIBUTION_ID  = credentials('STAGING_CLOUDFRONT_DISTRIBUTION_ID')
    PROD_CLOUDFRONT_DISTRIBUTION_ID     = credentials('PROD_CLOUDFRONT_DISTRIBUTION_ID')
    
    // Git configuration
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
    
    // Slack configuration (make optional)
    SLACK_CHANNEL = credentials('SLACK_CHANNEL')
  }
  
  stages {
    stage('Initialize') {
      steps {
        script {
          try {
            // Load environment configuration
            env.CONFIG = getEnvironmentConfig()
            env.TARGET_ENV = env.CONFIG.env
            
            // Set environment-specific variables
            env.CURRENT_ECR_REGISTRY = "${env.CONFIG.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
            env.CURRENT_API_URL = env.CONFIG.apiUrl
            env.CURRENT_FRONTEND_BUCKET = env.CONFIG.frontendBucket ?: ''
            env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID = env.CONFIG.cloudfrontDistributionId ?: ''
            env.DEPLOY_ENABLED = env.CONFIG.deployEnabled.toString()
            env.BUILD_IMAGES = env.CONFIG.buildImages.toString()
            
            // Validate required environment variables before proceeding
            if (env.TARGET_ENV == 'dev' && (!env.CONFIG.cloudfrontDistributionId || env.CONFIG.cloudfrontDistributionId?.trim() == '')) {
              error("DEV_CLOUDFRONT_DISTRIBUTION_ID is required but not set or empty")
            }
            
            echo """
              ========================================
              JENKINS CI/CD PIPELINE
              ========================================
              Branch: ${env.BRANCH_NAME}
              Target Environment: ${env.TARGET_ENV}
              ECR Registry: ${env.CURRENT_ECR_REGISTRY}
              Deploy Enabled: ${env.DEPLOY_ENABLED}
              Build Images: ${env.BUILD_IMAGES}
              CloudFront ID Configured: ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID ? 'Yes' : 'No'}
              ========================================
            """
          } catch (Exception e) {
            echo "ERROR in initialization: ${e.message}"
            currentBuild.result = 'FAILURE'
            error(e.message)
          }
        }
      }
    }
    
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
          env.GIT_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
        }
      }
    }
    
    stage('Detect Changed Services') {
      when {
        expression { env.BUILD_IMAGES == 'true' }
      }
      steps {
        script {
          if (env.BUILD_IMAGES == 'true') {
            def changed = detectChangedServices()
            env.CHANGED_SERVICES = changed.join(',')
            echo "Changed services detected: ${env.CHANGED_SERVICES}"
          }
        }
      }
    }
    
    stage('Authenticate to AWS') {
      when {
        expression { env.BUILD_IMAGES == 'true' && env.CONFIG.awsCredentialsId }
      }
      steps {
        script {
          withAWS(credentials: env.CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            sh """
              aws ecr get-login-password --region ${env.AWS_REGION} \
                | docker login --username AWS --password-stdin ${env.CURRENT_ECR_REGISTRY}
            """
          }
        }
      }
    }
    
    stage('SAST (SonarCloud)') {
      when {
        expression { env.CHANGED_SERVICES }
      }
      steps {
        script {
          withSonarQubeEnv('sonarcloud-token') {
            def services = env.CHANGED_SERVICES.split(',')
            services.each { serviceName ->
              def meta = serviceMap[serviceName]
              echo "Running SonarCloud SAST for ${serviceName}"
              dir(meta.path) {
                if (meta.type == 'maven') {
                  sh 'mvn verify sonar:sonar -DskipTests'
                } else if (meta.type == 'node') {
                  sh 'npm run sonar || echo "npm run sonar not defined"'
                }
              }
            }
          }
        }
      }
    }
    
    stage('Build and Push Docker Images') {
      when {
        expression { env.BUILD_IMAGES == 'true' && env.CHANGED_SERVICES }
      }
      steps {
        script {
          def services = env.CHANGED_SERVICES.split(',')
          
          services.each { serviceName ->
            def meta = serviceMap[serviceName]
            def fullImageName = "${env.IMAGE_NAMESPACE}/${meta.image}"
            def imageTag = "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.IMAGE_TAG}"
            
            echo "Building service: ${serviceName} (${meta.type})"
            
            dir(meta.path) {
              // Build Docker image
              sh "docker build -t ${imageTag} ."
              
              // Security scan with Trivy
              sh "trivy image --severity ${env.TRIVY_SEVERITY} --exit-code 1 ${imageTag}"
              
              // Push image to ECR
              sh "docker push ${imageTag}"
              
              // Tag with environment-specific tags
              if (env.TARGET_ENV == 'dev') {
                sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:latest"
                sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:latest"
                sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:dev-${env.GIT_COMMIT_SHORT}"
                sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:dev-${env.GIT_COMMIT_SHORT}"
              }
              
              if (env.TARGET_ENV == 'staging') {
                sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:staging-latest"
                sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:staging-latest"
                sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:staging-${env.GIT_COMMIT_SHORT}"
                sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:staging-${env.GIT_COMMIT_SHORT}"
              }
              
              if (env.TARGET_ENV == 'prod') {
                sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:prod-latest"
                sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:prod-latest"
                sh "docker tag ${imageTag} ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:prod-${env.GIT_COMMIT_SHORT}"
                sh "docker push ${env.CURRENT_ECR_REGISTRY}/${fullImageName}:prod-${env.GIT_COMMIT_SHORT}"
              }
            }
          }
        }
      }
    }
    
    stage('Update Kubernetes Manifests') {
      when {
        expression { 
          env.DEPLOY_ENABLED == 'true' && 
          env.CONFIG.kustomizeOverlay && 
          env.CHANGED_SERVICES 
        }
      }
      steps {
        script {
          withAWS(credentials: env.CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            def services = env.CHANGED_SERVICES.split(',')
            
            services.each { serviceName ->
              def meta = serviceMap[serviceName]
              def fullImageName = "${env.IMAGE_NAMESPACE}/${meta.image}"
              def newImage = "${env.CURRENT_ECR_REGISTRY}/${fullImageName}:${env.IMAGE_TAG}"
              
              dir(env.CONFIG.kustomizeOverlay) {
                sh "kustomize edit set image ${meta.image}=${newImage}"
              }
            }
            
            // Commit and push kustomize changes
            sh """
              git config user.email "jenkins@cinevision.com"
              git config user.name "Jenkins CI"
              git add ${env.CONFIG.kustomizeOverlay}
              git commit -m "[CI] Update images for ${env.TARGET_ENV} - Build ${env.BUILD_NUMBER}" || echo "No changes to commit"
              git push origin ${env.BRANCH_NAME}
            """
          }
        }
      }
    }
    
    stage('Deploy via ArgoCD') {
      when {
        expression { 
          env.DEPLOY_ENABLED == 'true' && 
          env.CONFIG.argocdApp && 
          env.CHANGED_SERVICES 
        }
      }
      steps {
        script {
          withAWS(credentials: env.CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            // Sync ArgoCD application
            sh "argocd app sync ${env.CONFIG.argocdApp} --grpc-web --prune"
            
            // Wait for sync to complete
            sh "argocd app wait ${env.CONFIG.argocdApp} --health --operation --timeout 300"
          }
        }
      }
    }
    
    stage('Integration Tests') {
      when {
        expression { 
          env.CONFIG.runIntegrationTests == true && 
          env.DEPLOY_ENABLED == 'true' &&
          env.CHANGED_SERVICES 
        }
      }
      agent {
        docker {
          image 'node:20-alpine'
          reuseNode true
        }
      }
      steps {
        script {
          dir('tests/integration') {
            sh 'npm ci'
            sh "BASE_URL=${env.CURRENT_API_URL} npm test"
          }
        }
      }
      post {
        always {
          archiveArtifacts artifacts: 'tests/integration/test-results/**/*', allowEmptyArchive: true
        }
      }
    }
    
    stage('Blue-Green Traffic Shift') {
      when {
        expression { env.TARGET_ENV == 'prod' && env.DEPLOY_ENABLED == 'true' }
      }
      steps {
        script {
          withAWS(credentials: env.CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            echo "Shifting traffic to green deployment"
            sh "kubectl -n ${env.CONFIG.namespace} apply -k ${env.PROD_GREEN_OVERLAY}"
            
            // Wait for traffic shift to complete
            sh "kubectl -n ${env.CONFIG.namespace} rollout status deployment -l app=cinevision --timeout=5m"
          }
        }
      }
    }
    
    stage('Performance Tests') {
      when {
        expression { 
          env.CONFIG.runPerformanceTests == true && 
          env.TARGET_ENV == 'staging' 
        }
      }
      steps {
        script {
          sh "k6 run tests/performance/smoke-test.js -e BASE_URL=${env.STAGING_API_URL}"
        }
      }
      post {
        always {
          archiveArtifacts artifacts: 'tests/performance/results/**/*', allowEmptyArchive: true
        }
      }
    }
    
    stage('Frontend Deployment') {
      when {
        expression {
          env.DEPLOY_ENABLED == 'true' &&
          env.CHANGED_SERVICES?.split(',')?.contains('frontend') &&
          env.CURRENT_FRONTEND_BUCKET
        }
      }
      steps {
        script {
          withAWS(credentials: env.CONFIG.awsCredentialsId, region: env.AWS_REGION) {
            dir('frontend') {
              // Build frontend
              sh 'npm ci'
              sh 'npm run build'
              
              // Sync to S3
              sh "aws s3 sync dist s3://${env.CURRENT_FRONTEND_BUCKET} --delete --exact-timestamps"
              
              // Invalidate CloudFront cache only if distribution ID is provided
              if (env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID && env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID.trim() != '') {
                sh "aws cloudfront create-invalidation --distribution-id ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID} --paths '/*'"
              } else {
                echo "WARNING: No CloudFront distribution ID provided - skipping cache invalidation"
              }
            }
          }
        }
      }
    }
    
    stage('Smoke Tests') {
      when {
        expression { 
          env.DEPLOY_ENABLED == 'true' && 
          env.CHANGED_SERVICES &&
          fileExists('tests/smoke')
        }
      }
      steps {
        script {
          dir('tests/smoke') {
            sh 'npm ci'
            sh "npm test -- --env=${env.TARGET_ENV} --services=${env.CHANGED_SERVICES}"
          }
        }
      }
    }
    
    stage('Approval for Production') {
      when {
        expression { 
          env.TARGET_ENV == 'prod' && 
          env.CONFIG.approvalRequired == true &&
          env.DEPLOY_ENABLED == 'true'
        }
      }
      steps {
        input message: "Deploy to PRODUCTION?",
              ok: "Deploy to Production",
              parameters: [
                string(name: 'DEPLOY_NOTES', defaultValue: '', description: 'Deployment notes or rollback plan'),
                choice(name: 'CONFIRMATION', choices: ['YES', 'NO'], description: 'Confirm production deployment')
              ]
      }
    }
    
    stage('Git Tag Release') {
      when {
        expression { env.TARGET_ENV == 'prod' && env.DEPLOY_ENABLED == 'true' }
      }
      steps {
        script {
          withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
            sh """
              git tag -a release-${env.IMAGE_TAG} -m "Release ${env.IMAGE_TAG} - Build ${env.BUILD_NUMBER}"
              git push https://\${GITHUB_TOKEN}@github.com/${env.GITHUB_REPO}.git release-${env.IMAGE_TAG}
            """
          }
        }
      }
    }
  }
  
  post {
    success {
      script {
        def duration = currentBuild.durationString
        
        echo """
          ========================================
          ✅ PIPELINE SUCCESSFUL
          ========================================
          Branch: ${env.BRANCH_NAME}
          Environment: ${env.TARGET_ENV}
          Duration: ${duration}
          Services Built: ${env.CHANGED_SERVICES}
          Image Tag: ${env.IMAGE_TAG}
          Commit: ${env.GIT_COMMIT_SHORT}
          ========================================
        """
        
        // Send Slack notification for production deployments (only if credentials are available)
        if (env.TARGET_ENV == 'prod') {
          try {
            slackSend(
              color: 'good',
              message: """
                ✅ Production Deployment Successful
                Branch: ${env.BRANCH_NAME}
                Services: ${env.CHANGED_SERVICES}
                Image: ${env.IMAGE_TAG}
                Build: ${env.BUILD_URL}
              """.stripIndent()
            )
          } catch (Exception e) {
            echo "Slack notification failed (non-fatal): ${e.message}"
          }
        }
        
        // Archive test results
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/test-results/**/*.xml'
      }
    }
    
    failure {
      script {
        echo """
          ❌ Pipeline FAILED
          Branch: ${env.BRANCH_NAME}
          Environment: ${env.TARGET_ENV}
          Build: ${env.BUILD_URL}
          Services: ${env.CHANGED_SERVICES}
        """
        
        // Try to send Slack notification but don't fail if it doesn't work
        try {
          slackSend(
            color: 'danger',
            message: """
              ❌ Pipeline FAILED
              Branch: ${env.BRANCH_NAME}
              Environment: ${env.TARGET_ENV}
              Build: ${env.BUILD_URL}
              Services: ${env.CHANGED_SERVICES}
            """.stripIndent()
          )
        } catch (Exception e) {
          echo "Slack notification failed (non-fatal): ${e.message}"
        }
        
        // Archive error logs (only if we can access files)
        try {
          archiveArtifacts artifacts: '**/hs_err_pid*.log, **/error.log, **/target/surefire-reports/*.xml', 
                         allowEmptyArchive: true
        } catch (Exception e) {
          echo "Failed to archive artifacts: ${e.message}"
        }
      }
    }
    
    always {
      // Clean up workspace - wrapped in script block with error handling
      script {
        try {
          echo "Starting workspace cleanup..."
          cleanWs(
            cleanWhenNotBuilt: false,
            deleteDirs: true,
            disableDeferredWipeout: false
          )
          echo "Workspace cleanup completed successfully"
        } catch (Exception e) {
          echo "WARNING: Workspace cleanup failed - ${e.message}"
          echo "This may be due to file permissions or locked files"
          // Try alternative cleanup method
          try {
            sh 'rm -rf * || true'
            echo "Alternative cleanup completed"
          } catch (Exception e2) {
            echo "Alternative cleanup also failed: ${e2.message}"
          }
        }
      }
    }
  }
}