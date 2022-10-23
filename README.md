# CurrencyConverter - instrukcja

1. Zbuduj aplikację za pomoca polecenia 'mvn clean install'
2. Uruchom aplikację
3. Przeprowadź testy np. za pomocą narzędzia POSTMAN:
## Metoda GET

## Endpoint:
http://localhost:8080/currencyConvert
w body dodaj json:

## Przykłady wywołań:

- niepoprawne:
{
"sourceCurrencyCode": "USD",
"amountInSourceCurrency": 25,
"targetCurrencyCode": "EUR",
"conversionDate": "2022-10-09"
}

- poprawne:
{
"sourceCurrencyCode": "GBP",
"amountInSourceCurrency": 25,
"targetCurrencyCode": "USD",
"conversionDate": "2022-10-13"
}