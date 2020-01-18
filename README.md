# tangem-pending-coffee

This project aims to implementing [pending coffee](https://en.wikipedia.org/wiki/Caff%C3%A8_sospeso) functionality using the Tangem SDK and smart contracts.

The intended use case is as follows:

1.) Using the Tangem SDK, customers purchase some number of coffees and some number of pending coffees. The customer doesn't receive their pending coffees, despite paying for them. This uses the Tangem SDK to send the cryptocurrency to a smart contract that forwards the appropriate amount to the café's wallet and retains the amount for pending coffees.

2.) Later on, the café can give the pending coffees away for free to those less fortunate who come in asking for a coffee. When they do so, they withdraw the cost of the pending coffee from the smart contract.

3.) The original customer gets notified that someone availed of their pending coffee.
