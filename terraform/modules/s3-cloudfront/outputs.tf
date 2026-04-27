output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "logs_bucket_name" {
  value = aws_s3_bucket.logs.bucket
}

output "dr_frontend_bucket_name" {
  value = try(aws_s3_bucket.frontend_dr[0].bucket, null)
}

output "cloudfront_domain_name" {
  value = aws_cloudfront_distribution.frontend.domain_name
}

output "cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.frontend.id
}
