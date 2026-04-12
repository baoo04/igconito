# Payment Service

Mock gateway rules: amount &gt; 10,000,000 VND fails, card ending **0000** fails, ~10% random failure, otherwise success.

`POST /payments` returns **201** on success or **402** when declined (transaction still stored as `FAILED`).
