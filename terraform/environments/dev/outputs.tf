output "vpc_id" {
  value = module.vpc.vpc_id
}

output "frontend_bucket_name" {
  value = module.s3_cloudfront.frontend_bucket_name
}

output "cloudfront_domain_name" {
  value = module.s3_cloudfront.cloudfront_domain_name
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
