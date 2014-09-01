param($exchangeUri)
                 
$session_config_name = "Microsoft.Exchange"

$session = (Get-PSSession | where { ($_.ConfigurationName -eq $session_config_name) -and ($_.State -eq "Opened") })
if ($session) {
    return $session
}

$session = New-PSSession `
    -ConfigurationName $session_config_name `
    -ConnectionUri $exchangeUri `
    -Authentication Kerberos `
    -AllowRedirection

Import-PSSession $Session

return $session
