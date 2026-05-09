// ============================================
// JENKINS PIPELINE FOR POST-MERGE CI/CD
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
  def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim().split('\n') as List
  def changed = []
  
  serviceMap.each { serviceName, meta ->
    if (changedFiles.any { it.startsWith("${meta.path}/") }) {
      changed << serviceName
    }
  }
  
  if (changed.isEmpty() || changedFiles.isEmpty()) {
    echo "No specific service changes detected - building all services"
    changed = serviceMap.keySet() as List
  }
  
  return changed.unique()
}

def getEnvironmentConfig() {
  def branch = env.BRANCH_NAME
  
  if (branch == 'main' || branch == 'master' || branch.startsWith('hotfix/')) {
    return [
      env: 'prod',
      awsAccountId: env.PROD_AWS_ACCOUNT_ID,
      awsCredentialsId: 'aws-prod-credentials',
      namespace: 'cinevision-prod',
      argocdApp: 'cinevision-prod-green',
      frontendBucket: env.PROD_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.PROD_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket: env.PROD_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId: env.PROD_POSTER_CLOUDFRONT_ID,
      archiveBucket: env.PROD_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId: env.PROD_ARCHIVE_CLOUDFRONT_ID,
      apiUrl: env.PROD_API_URL,
      kustomizeOverlay: env.PROD_GREEN_OVERLAY,
      deployEnabled: true,
      approvalRequired: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
  if (branch.startsWith('release/')) {
    return [
      env: 'staging',
      awsAccountId: env.STAGING_AWS_ACCOUNT_ID,
      awsCredentialsId: 'aws-staging-credentials',
      namespace: 'cinevision-staging',
      argocdApp: 'cinevision-staging',
      frontendBucket: env.STAGING_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.STAGING_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket: env.STAGING_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId: env.STAGING_POSTER_CLOUDFRONT_ID,
      archiveBucket: env.STAGING_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId: env.STAGING_ARCHIVE_CLOUDFRONT_ID,
      apiUrl: env.STAGING_API_URL,
      kustomizeOverlay: env.STAGING_OVERLAY,
      deployEnabled: true,
      approvalRequired: true,
      runPerformanceTests: true,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
  if (branch == 'develop') {
    return [
      env: 'dev',
      awsAccountId: env.DEV_AWS_ACCOUNT_ID,
      awsCredentialsId: 'ecr-eks',
      namespace: 'cinevision-dev',
      argocdApp: 'cinevision-dev',
      frontendBucket: env.DEV_FRONTEND_BUCKET,
      cloudfrontDistributionId: env.DEV_CLOUDFRONT_DISTRIBUTION_ID,
      posterBucket: env.DEV_MOVIE_POSTERS_BUCKET,
      posterCloudfrontId: env.DEV_POSTER_CLOUDFRONT_ID,
      archiveBucket: env.DEV_EMAIL_ARCHIVES_BUCKET,
      archiveCloudfrontId: env.DEV_ARCHIVE_CLOUDFRONT_ID,
      apiUrl: env.DEV_API_URL,
      kustomizeOverlay: env.DEV_OVERLAY,
      deployEnabled: true,
      approvalRequired: false,
      runPerformanceTests: false,
      runIntegrationTests: true,
      buildImages: true
    ]
  }
  
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
  agent any
  
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    timeout(time: 60, unit: 'MINUTES')
  }
  
  environment {
    AWS_REGION = 'us-east-1'
    DR_AWS_REGION = 'us-west-2'
    
    DEV_AWS_ACCOUNT_ID     = credentials('DEV_AWS_ACCOUNT_ID')
    STAGING_AWS_ACCOUNT_ID = credentials('STAGING_AWS_ACCOUNT_ID')
    PROD_AWS_ACCOUNT_ID    = credentials('PROD_AWS_ACCOUNT_ID')
    
    IMAGE_NAMESPACE = 'cinevision'
    
    DEV_OVERLAY        = 'k8s/overlays/dev'
    STAGING_OVERLAY    = 'k8s/overlays/staging'
    PROD_BLUE_OVERLAY  = 'k8s/overlays/prod/blue'
    PROD_GREEN_OVERLAY = 'k8s/overlays/prod/green'
    
    DEV_API_URL      = 'https://dev-api.cinevisionca.link'
    STAGING_API_URL  = 'https://staging-api.cinevision.com'
    PROD_API_URL     = 'https://api.cinevision.com'
    
    TRIVY_SEVERITY = 'HIGH,CRITICAL'
    
    DEV_FRONTEND_BUCKET      = 'dev-cinevision-dev-frontend'
    STAGING_FRONTEND_BUCKET  = 'staging-cinevision-staging-frontend'
    PROD_FRONTEND_BUCKET     = 'prod-cinevision-prod-frontend'
    
    DEV_MOVIE_POSTERS_BUCKET      = 'dev-cinevision-dev-movie-posters'
    STAGING_MOVIE_POSTERS_BUCKET  = 'staging-cinevision-staging-movie-posters'
    PROD_MOVIE_POSTERS_BUCKET     = 'prod-cinevision-prod-movie-posters'
    
    DEV_EMAIL_ARCHIVES_BUCKET     = 'dev-cinevision-dev-email-archives'
    STAGING_EMAIL_ARCHIVES_BUCKET = 'staging-cinevision-staging-email-archives'
    PROD_EMAIL_ARCHIVES_BUCKET    = 'prod-cinevision-prod-email-archives'
    
    DEV_CLOUDFRONT_DISTRIBUTION_ID      = credentials('DEV_CLOUDFRONT_DISTRIBUTION_ID')
    DEV_POSTER_CLOUDFRONT_ID           = credentials('DEV_POSTER_CLOUDFRONT_ID')
    DEV_ARCHIVE_CLOUDFRONT_ID          = credentials('DEV_ARCHIVE_CLOUDFRONT_ID')
    
    STAGING_CLOUDFRONT_DISTRIBUTION_ID  = credentials('STAGING_CLOUDFRONT_DISTRIBUTION_ID')
    STAGING_POSTER_CLOUDFRONT_ID       = credentials('STAGING_POSTER_CLOUDFRONT_ID')
    STAGING_ARCHIVE_CLOUDFRONT_ID      = credentials('STAGING_ARCHIVE_CLOUDFRONT_ID')
    
    PROD_CLOUDFRONT_DISTRIBUTION_ID     = credentials('PROD_CLOUDFRONT_DISTRIBUTION_ID')
    PROD_POSTER_CLOUDFRONT_ID          = credentials('PROD_POSTER_CLOUDFRONT_ID')
    PROD_ARCHIVE_CLOUDFRONT_ID         = credentials('PROD_ARCHIVE_CLOUDFRONT_ID')
    
    GITHUB_REPO = 'functionalprojects/cinevision-pro'
  }
  
  stages {
    stage('Initialize') {
      steps {
        script {
          env.CONFIG = getEnvironmentConfig()
          env.TARGET_ENV = env.CONFIG.env
          
          env.CURRENT_ECR_REGISTRY = "${env.CONFIG.awsAccountId}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
          env.CURRENT_API_URL = env.CONFIG.apiUrl
          env.CURRENT_FRONTEND_BUCKET = env.CONFIG.frontendBucket ?: ''
          env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID = env.CONFIG.cloudfrontDistributionId ?: ''
          
          env.CURRENT_POSTER_BUCKET = env.CONFIG.posterBucket ?: ''
          env.CURRENT_POSTER_CLOUDFRONT_ID = env.CONFIG.posterCloudfrontId ?: ''
          
          env.CURRENT_ARCHIVE_BUCKET = env.CONFIG.archiveBucket ?: ''
          env.CURRENT_ARCHIVE_CLOUDFRONT_ID = env.CONFIG.archiveCloudfrontId ?: ''
          
          env.DEPLOY_ENABLED = env.CONFIG.deployEnabled.toString()
          env.BUILD_IMAGES = env.CONFIG.buildImages.toString()
          
          // Critical validation
          if (env.TARGET_ENV == 'dev') {
            def cloudfrontId = env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID
            if (!cloudfrontId || cloudfrontId.trim() == '' || cloudfrontId == '****') {
              error("DEV_CLOUDFRONT_DISTRIBUTION_ID is not properly configured in Jenkins credentials")
            }
            echo "CloudFront Distribution ID found: ${cloudfrontId}"
          }
          
          echo """
            ========================================
            JENKINS CI/CD PIPELINE
            ========================================
            Branch: ${env.BRANCH_NAME}
            Environment: ${env.TARGET_ENV}
            Deploy Enabled: ${env.DEPLOY_ENABLED}
            Build Images: ${env.BUILD_IMAGES}
            ========================================
          """
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
          def changed = detectChangedServices()
          env.CHANGED_SERVICES = changed.join(',')
          echo "Changed services: ${env.CHANGED_SERVICES}"
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
            
            echo "Building ${serviceName}"
            
            dir(meta.path) {
              sh "docker build -t ${imageTag} ."
              sh "trivy image --severity ${env.TRIVY_SEVERITY} --exit-code 1 ${imageTag}"
              sh "docker push ${imageTag}"
            }
          }
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
              sh 'npm ci'
              sh 'npm run build'
              sh "aws s3 sync dist s3://${env.CURRENT_FRONTEND_BUCKET} --delete"
              
              if (env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID && env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID != '****' && env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID != '') {
                sh "aws cloudfront create-invalidation --distribution-id ${env.CURRENT_CLOUDFRONT_DISTRIBUTION_ID} --paths '/*'"
              }
              
              if (env.CHANGED_SERVICES?.split(',')?.contains('movieService') && env.CURRENT_POSTER_CLOUDFRONT_ID && env.CURRENT_POSTER_CLOUDFRONT_ID != '****' && env.CURRENT_POSTER_CLOUDFRONT_ID != '') {
                sh "aws cloudfront create-invalidation --distribution-id ${env.CURRENT_POSTER_CLOUDFRONT_ID} --paths '/*'"
              }
              if (env.CHANGED_SERVICES?.split(',')?.contains('emailService') && env.CURRENT_ARCHIVE_CLOUDFRONT_ID && env.CURRENT_ARCHIVE_CLOUDFRONT_ID != '****' && env.CURRENT_ARCHIVE_CLOUDFRONT_ID != '') {
                sh "aws cloudfront create-invalidation --distribution-id ${env.CURRENT_ARCHIVE_CLOUDFRONT_ID} --paths '/*'"
              }
            }
          }
        }
      }
    }
  }
  
  post {
    always {
      // Post-actions now run WITHIN the node context
      script {
        echo "=== POST-BUILD CLEANUP ==="
        try {
          cleanWs(
            cleanWhenNotBuilt: false,
            deleteDirs: true,
            disableDeferredWipeout: false
          )
          echo "Workspace cleaned successfully"
        } catch (Exception e) {
          echo "Cleanup warning: ${e.message}"
        }
      }
    }
    
    success {
      script {
        echo """
          ✅ SUCCESS: ${env.BRANCH_NAME} -> ${env.TARGET_ENV}
          Build: ${env.BUILD_URL}
        """
      }
    }
    
    failure {
      script {
        echo """
          ❌ FAILED: ${env.BRANCH_NAME} -> ${env.TARGET_ENV}
          Build: ${env.BUILD_URL}
        """
        
        // Optional: Archive artifacts only if they exist
        try {
          archiveArtifacts artifacts: '**/target/surefire-reports/*.xml', allowEmptyArchive: true
        } catch (Exception e) {
          echo "No artifacts to archive"
        }
      }
    }
  }
}