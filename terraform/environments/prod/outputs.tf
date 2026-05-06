output "primary_vpc_id" {
  value = module.vpc.vpc_id
}

output "dr_vpc_id" {
  value = module.dr_vpc.vpc_id
}

output "cloudfront_domain_name" {
  value = module.s3_cloudfront.cloudfront_domain_name
}

output "frontend_bucket_name" {
  value = module.s3_cloudfront.frontend_bucket_name
}

output "dr_frontend_bucket_name" {
  value = module.s3_cloudfront.dr_frontend_bucket_name
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "dr_private_subnet_ids" {
  value = module.dr_vpc.private_subnet_ids
}

output "dr_database_subnet_ids" {
  value = module.dr_vpc.database_subnet_ids
}

output "dr_eks_cluster_security_group_id" {
  value = module.dr_vpc.eks_cluster_security_group_id
}

output "dr_app_nodes_security_group_id" {
  value = module.dr_vpc.app_nodes_security_group_id
}

output "dr_redis_security_group_id" {
  value = module.dr_vpc.redis_security_group_id
}

output "primary_postgres_endpoint" {
  value = module.rds.primary_endpoint
}

output "dr_postgres_replica_endpoint" {
  value = module.rds.dr_replica_endpoint
}

output "primary_documentdb_endpoint" {
  value = module.documentdb.primary_cluster_endpoint
}

output "dr_documentdb_endpoint" {
  value = module.documentdb.dr_cluster_endpoint
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
