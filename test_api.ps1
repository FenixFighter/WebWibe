$body = @{
    message = "How do I apply for a credit card?"
    category = "Banking"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/question" -Method POST -ContentType "application/json" -Body $body
$response | ConvertTo-Json

