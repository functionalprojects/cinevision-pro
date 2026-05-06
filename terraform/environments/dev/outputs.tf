output "movie_posters_cloudfront_domain_name" {
  value = module.movie_posters_cloudfront.cloudfront_domain_name
}

output "movie_posters_cloudfront_distribution_id" {
  value = module.movie_posters_cloudfront.cloudfront_distribution_id
}

output "email_archives_cloudfront_domain_name" {
  value = module.email_archives_cloudfront.cloudfront_domain_name
}

output "email_archives_cloudfront_distribution_id" {
  value = module.email_archives_cloudfront.cloudfront_distribution_id
}

output "vpc_id" {
  value = module.vpc.vpc_id
}

output "frontend_bucket_name" {
  value = module.s3_cloudfront.frontend_bucket_name
}

output "cloudfront_domain_name" {
  value = module.s3_cloudfront.cloudfront_domain_name
}

output "cloudfront_distribution_id" {
  value = module.s3_cloudfront.cloudfront_distribution_id
}

output "movie_posters_bucket_name" {
  value = module.movie_posters_s3.bucket_name
}

output "email_archives_bucket_name" {
  value = module.email_archives_s3.bucket_name
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "postgres_endpoint" {
  value = module.rds.primary_endpoint
}

output "documentdb_endpoint" {
  value = module.documentdb.primary_cluster_endpoint
}

output "redis_endpoint" {
  value = module.redis.primary_endpoint_address
}

output "msk_bootstrap_brokers_tls" {
  value = module.msk.bootstrap_brokers_tls
}

output "database_credentials_secret_name" {
  value = var.database_credentials_secret_name
}

output "application_secrets_secret_name" {
  value = data.aws_secretsmanager_secret.application_secrets.name
}

output "application_secrets_secret_arn" {
  value = data.aws_secretsmanager_secret.application_secrets.arn
}
