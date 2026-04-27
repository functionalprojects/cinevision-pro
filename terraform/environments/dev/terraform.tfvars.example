project_name           = "cinevision"
aws_profile            = "cinevision-dev"
primary_region         = "us-east-1"
dr_region              = "us-west-2"
cidr_block             = "10.10.0.0/16"
azs                    = ["us-east-1a", "us-east-1b", "us-east-1c"]
public_subnet_cidrs    = ["10.10.0.0/24", "10.10.1.0/24", "10.10.2.0/24"]
private_subnet_cidrs   = ["10.10.10.0/24", "10.10.11.0/24", "10.10.12.0/24"]
database_subnet_cidrs  = ["10.10.20.0/24", "10.10.21.0/24", "10.10.22.0/24"]
frontend_bucket_name   = "BUCKET_NAME-cinevision-dev-frontend"
logs_bucket_name       = "BUCKET_NAME-cinevision-dev-logs"
frontend_aliases       = ["dev.DOMAIN_NAME"]
acm_certificate_arn    = "arn:aws:acm:us-east-1:ACCOUNT_ID:certificate/DEV_CERTIFICATE_ID"
rds_instance_class     = "db.t4g.medium"
db_name                = "cinevision"
db_username            = "cinevision_admin"
db_password            = "REPLACE_WITH_SECRET_OR_SECRETS_MANAGER_VALUE"
docdb_master_username  = "cinevision_docdb_admin"
docdb_master_password  = "REPLACE_WITH_SECRET_OR_SECRETS_MANAGER_VALUE"
redis_node_type        = "cache.t4g.small"
msk_broker_count       = 3
msk_broker_instance_type = "kafka.t3.small"
node_groups = {
  general = {
    instance_types = ["t3.large"]
    desired_size   = 2
    min_size       = 2
    max_size       = 4
    disk_size      = 50
    capacity_type  = "ON_DEMAND"
  }
}
tags = {
  Owner = "platform-team"
}
