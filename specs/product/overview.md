# Product Overview

## What this is

A shared backend for a restaurant management system, built as Phase 2 of the
FIAP Pos Tech "Arquitetura e Desenvolvimento Java" Tech Challenge. It lets
restaurant owners manage their establishments and lets customers browse them.

## Actors

- **Dono de Restaurante** (Restaurant Owner): registers and manages their own
  restaurant(s) and menu items.
- **Cliente** (Customer): browses restaurants and menu items.

Both actors are represented by the same `User` aggregate, distinguished by
`UserType`. Only a user of type "Dono de Restaurante" may own a restaurant
(business rule to be confirmed and enforced starting with the Restaurant
module).

## Phase 2 scope

Four aggregates, delivered incrementally across modules (see
`specs/modules/`):

| Aggregate | Summary |
|---|---|
| User | Base user with auth credentials. Full CRUD. JWT-secured. |
| UserType | "Dono de Restaurante" \| "Cliente". CRUD + association to a User. |
| Restaurant | Name, address, cuisine type, opening hours, owner (an existing User). Full CRUD. |
| MenuItem | Name, description, price, dine-in-only flag, photo path (string). Full CRUD, belongs to a Restaurant. |

M00 (this bootstrap) intentionally contains none of the above business logic
— see `specs/modules/00-bootstrap.md`.
